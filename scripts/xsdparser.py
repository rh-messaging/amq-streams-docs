#!/usr/bin/python
# coding: utf-8

import string
import uuid
import getopt
import sys
from lxml import etree
from io import BytesIO
from operator import itemgetter

schema_space = '{http://www.w3.org/2001/XMLSchema}'
root = etree.parse('artemis-configuration.xsd')
documentation_path = 'xsd:annotation/xsd:documentation'
enumeration_path = 'xsd:simpleType/xsd:restriction/xsd:enumeration'
attribute_paths = [
    'xsd:attribute',
    'xsd:complexType/xsd:attribute',
    'xsd:simpleContent/xsd:extension/xsd:attribute'
]
child_xpaths = {
    'all':[
        'xsd:all/xsd:element',
        'xsd:complexType/xsd:all/xsd:element'],
    'sequence':[
        'xsd:complexType/xsd:sequence/xsd:element',
        'xsd:sequence/xsd:element'],
    'sequence_choice':[
        'xsd:sequence/xsd:choice/xsd:element'],
    'choice':[
        'xsd:choice/xsd:element',
        'xsd:complexType/xsd:choice/xsd:element']
}

#select all book elements that contain a child title element (regardless of how deep it is nested) containing language attribute value equal to 'it'.
# book[descendant::title[@lang='it']]

# All element specifies that the child elements can appear in any order and that each child element can occur zero or one time.

# Choice element allows only one of the elements contained in the <choice> declaration to be present within the containing element.

# Sequence element specifies that the child elements must appear in a sequence. Each child element can occur from 0 to any number of times.

class MyElement:
    children_of_all = []
    children_of_sequence = []
    children_of_choice = []
    children_of_sequence_choice = []
    is_parent = False
    def __init__(self, eid, name, myparent, myparenteid, attributes, options, default, required, description, source, occurs):
        self.eid = eid
        self.name = name
        self.myparent = myparent
        self.myparenteid = myparenteid
        self.description = description
        self.attributes = attributes
        self.options = options
        self.default = default
        self.required = required
        self.source = source
        self.occurs = occurs
    def __repr__(self):
        return repr((self.eid,self.name,self.myparent,self.myparenteid,self.description,self.options,self.default,self.required,self.children,self.children_occurs,self.source,self.occurs))

class MyAttribute:
    def __init__(self, name, required, description):
        self.name = name
        self.description = description
        self.required = required
    def __repr__(self):
        return repr((self.name,self.description,self.required))

def get_attributes(elem):
    attr_list = []
    for path in attribute_paths:
        attrs = elem.findall(path.replace('xsd:', schema_space))
        if attrs is not None and len(attrs) > 0:
            for a in attrs:
                    attr = MyAttribute(
                        get_name(a),
                        get_required(a),
                        get_documentation(a)
                    )
                    attr_list.append(attr)
    return attr_list

def get_enumeration(elem):
    enums = elem.findall( enumeration_path.replace('xsd:', schema_space))
    vals = []
    if enums is not None and len(enums) > 0:
        for e in enums:
            vals.append(e.get('value'))
        return ', '.join(vals)
    else:
        return ''

def get_documentation(elem):
    doc = elem.find(documentation_path.replace('xsd:', schema_space))
    if doc is not None:
        description = doc.text.strip()
        return ' '.join(description.split())
    else:
        return ''

def get_required(elem):
    val = elem.get('use')
    if val is not None:
        if  val == 'required':
            return 'Required'
        if  val == 'optional':
            return 'Optional'
        if  val == 'prohibited':
            return 'Prohibited'
    val = elem.get('minOccurs')
    if val is not None and val == '1':
        return 'Required'
    return ''

def get_max_occurs(elem):
    val = elem.get('maxOccurs')
    if val is None or val == '1':
            return ' Use only one time.'
    if val is not None and val.lower() == 'unbounded':
            return ' Can repeat.'
    return ''

def get_default(elem):
    val = elem.get('default')
    if val is not None:
        return val
    else:
        return ''

def get_name(elem):
    name = elem.get('name')
    if name is None:
        name = ''
    return name

def get_ref(name):
    path = '//xsd:element[@name="' + name + '"]'
    return root.find(path.replace('xsd:', schema_space))

def get_type(name):
    path = '//xsd:complexType[@name="' + name + '"]'
    mytype = root.find(path.replace('xsd:', schema_space))
    if mytype is None:
        path = '//xsd:simpleType[@name="' + name + '"]'
        return root.find(path.replace('xsd:', schema_space))
    return mytype

def get_children(element, name, eid, xpaths):
    children = []
    for path in xpaths:
        results = element.findall(path.replace('xsd:', schema_space))
        if results is not None and len(results) > 0:
            for r in results:
                child = build_single(r)
                child.myparent = name
                child.myparenteid = eid
                children.append(child)
    return children

def get_core():
    elements = []
    core = root.findall("/xsd:complexType[@name='configurationType']/xsd:all/xsd:element".replace('xsd:', schema_space))
    for c in core:
        my_e = build_single(c)
        elements.append(my_e)
    return elements

def build_single(element):
    required = get_required(element)
    default = get_default(element)
    name = get_name(element)
    descripiton = get_documentation(element)
    occurs = get_max_occurs(element)
    element_ref = element.get('ref')
    element_type = element.get('type')
    myparent = ''
    myparenteid = ''
    source = ''
    if element_ref is not None:
        element = get_ref(element_ref)
        name = element_ref
    elif element_type is not None and 'xsd:' not in element_type:
        element = get_type(element_type)
    eid = name + str(uuid.uuid4())
    e = MyElement(
        eid,
        name,
        myparent,
        myparenteid,
        get_attributes(element),
        get_enumeration(element),
        default,
        required,
        descripiton,
        etree.tostring(element, pretty_print=True).strip(),
        occurs
    )
    e.children_of_all = get_children(element, name, eid, child_xpaths['all'])
    e.children_of_sequence = get_children(element, name, eid, child_xpaths['sequence'])
    e.children_of_sequence_choice = get_children(element, name, eid, child_xpaths['sequence_choice'])
    e.children_of_choice = get_children(element, name, eid, child_xpaths['choice'])
    return e

def justify(string, offset):
    return string.rjust(len(string) + offset,' ')

def clean_up(string):
    if len(string) > 0:
        string = string.capitalize()
        if string.endswith('.'):
            return string
        else:
            return string + '.'
    return string

def build_text(e):
    text = ''
    if e.required is not None and len(e.required) > 0:
        text = text + e.required + '. '
    if e.description is not None and len(e.options) > 0:
        text = text + clean_up(e.description)
    #else:
    #    text = "No description available."
    if e.options is not None and len(e.options) > 0:
        text = text + ' The valid options are: ' + e.options + '.'
    if e.default is not None and len(e.default) > 0:
        text = text + ' The default is ' + e.default + '.'
    text = text + e.occurs
    return text

def print_children_list_names_adoc(children):
    for c in children:
        print 'xref:' + c.eid + '[' + c.name + ']&nbsp;'

def print_children_list_adoc(e):
    print '>|Children'
    print '|'

    have_kids = False
    length = len(e.children_of_all)
    if e.children_of_all is not None and length > 0:
        print 'Use in any order(',
        print_children_list_names_adoc(e.children_of_all)
        print ')'
        have_kids = True

    length = len(e.children_of_sequence)
    if e.children_of_sequence is not None and length > 0:
        print ' Use in the order shown(',
        print_children_list_names_adoc(e.children_of_sequence)
        length = len(e.children_of_sequence_choice)
        if e.children_of_sequence_choice is not None and length > 0:
            print ' Use only one[',
            print_children_list_names_adoc(e.children_of_sequence_choice)
            print ']'
        print ')'
        have_kids = True

    length = len(e.children_of_choice)
    if e.children_of_choice is not None  and length > 0:
        print ' Use only one[',
        print_children_list_names_adoc(e.children_of_choice)
        print ']'
        have_kids = True

    if not have_kids:
        print 'None'
    print ''

def print_children_adoc(e):
    if e.children_of_all is not None and len(e.children_of_all) > 0:
        for c in e.children_of_all:
            print_element_adoc(c)

    if e.children_of_sequence is not None and len(e.children_of_sequence) > 0:
        for c in e.children_of_sequence:
            print_element_adoc(c)

    if e.children_of_sequence_choice is not None and len(e.children_of_sequence_choice) > 0:
        for c in e.children_of_sequence_choice:
            print_element_adoc(c)

    if e.children_of_choice is not None and len(e.children_of_choice) > 0:
        for c in e.children_of_choice:
            print_element_adoc(c)

def print_parent_adoc(e):
    print '>|Parent'
    if e.myparent is not None and len(e.myparent) > 0:
        print '|xref:' + e.myparenteid + '[' + e.myparent + ']'
    else:
        print '|None'

def print_source_adoc(e):
    print '>|Schema Source'
    print '|'
    print '[source]'
    print '----'
    print e.source
    print '----'

def print_attribute_adoc(attribute):
    text = ''
    text = text + clean_up(attribute.description)
    if attribute.required is not None and len(attribute.required) > 0:
        text = text + ' ' + attribute.required + '.'
    print '`' + attribute.name + '`.&nbsp;&nbsp;' + text

    print ''

def print_attributes_adoc(e):
    print '>|Attributes'
    if e.attributes is not None and len(e.attributes) > 0:
        print '|'
        for a in e.attributes:
            print_attribute_adoc(a)
    else:
        print '|None'

def print_description_adoc(e):
    text = build_text(e)
    print '>|Description'
    print '|' + text

def print_element_adoc(e):
    print '[[' + e.eid + ']]'
    print '[cols="15%s,85%a",caption="Element: ",options="nowrap"]'
    print '.' + e.name
    print '|==='
    print_description_adoc(e)
    print_attributes_adoc(e)
    print_children_list_adoc(e)
    print_parent_adoc(e)
    print_source_adoc(e)
    print '|==='
    print ''
    print_children_adoc(e)

def print_all_adoc():
    elements = get_core()
    elements.sort(key=lambda x:x.name)
    print '[[top]]'
    print '= Core Configuration Elements'
    print ''
    for e in elements:
        print_element_adoc(e)
    print ''

def format_csv(element, level):
    for x in range(1, 6):
        if x == level:
            print element.name,
        else:
            print ',',
    print ',' + build_text(element)

def print_element_csv(e, level=1):

    format_csv(e, level)

    if e.children_of_all is not None and len(e.children_of_all) > 0:
        e.children_of_all.sort(key=lambda x:x.name)
        for c in e.children_of_all:
            print_element_csv(c, level + 1)

    if e.children_of_sequence is not None and len(e.children_of_sequence) > 0:
        e.children_of_sequence.sort(key=lambda x:x.name)
        for c in e.children_of_sequence:
            print_element_csv(c, level + 1)

    if e.children_of_choice is not None and len(e.children_of_choice) > 0:
        e.children_of_choice.sort(key=lambda x:x.name)
        for c in e.children_of_choice:
            print_element_csv(c, level + 1)

def print_all_csv():
    elements = get_core()
    elements.sort(key=lambda x:x.name)
    print 'Top Level,Children,Grandchildren,Great-Grandchildren,Great-Great-Grandchildren,Description'
    for e in elements:
        print_element_csv(e)

def print_attribute_html(attribute, offset):
    text = ''
    if attribute.required:
        text = text + "Required. "
    text = text + clean_up(attribute.description)
    #if len(text) == 0:
    #    text = text + "No description available."
    print justify('<li class="file">' + attribute.name + '. <span id="description">' + text + '</span></li>', offset)

def print_children_html(children, message, offset):
    print justify('<li>', offset)
    my_id = str(uuid.uuid4())
    print justify('<label for="' + my_id + '"><span id="description">' + message + '</span></label>', offset + 3)
    print justify('<input type="checkbox" name="checkbox" checked id="' + my_id + '" />', offset + 3)
    print justify('<ul>', offset + 3)
    for c in children:
        print_element_html(c, offset + 6)
    print justify("</ul>", offset + 3)
    print justify("</li>", offset)

def print_element_html(e, offset=9):
    text = build_text(e)
    my_id = str(uuid.uuid4())
    if len(text) > 0:
        text = '&lt;' + e.name + '&gt; ' + '<span id="description">' + text + '</span>'
    else:
        text = e.name
    if len(e.children_of_all) > 0 or len(e.children_of_sequence) > 0 or len(e.children_of_choice) > 0 or len(e.attributes) > 0:
        print justify("<li>", offset)
        print justify('<label for="' + my_id + '"> ' + text + '</label>', offset + 3)
        print justify('<input type="checkbox" name="checkbox" checked id="' + my_id + '" />', offset + 3)
        print justify("<ul>", offset + 3)
    else:
        print justify('<li class="file">', offset)
        print justify(text, offset + 3)
        print justify("</li>", offset)

    if e.attributes is not None and len(e.attributes) > 0:
        print justify('<li>', offset + 6)
        my_id = str(uuid.uuid4())
        print justify('<label for="' + my_id + '"><span id="description">Attributes:</span></label>', offset + 9)
        print justify('<input type="checkbox" name="checkbox" checked enabled" id="' + my_id + '" />', offset + 9)
        print justify('<ul>', offset + 9)
        for a in e.attributes:
            print_attribute_html(a, offset + 6)
        print justify('</ul>', offset + 9)
        print justify("</li>", offset + 6)

    if e.children_of_all is not None and len(e.children_of_all) > 0:
        e.children_of_all.sort(key=lambda x:x.name)
        print_children_html(e.children_of_all, 'Children (use in any order):', offset + 6)

    if e.children_of_sequence is not None and len(e.children_of_sequence) > 0:
        print_children_html(e.children_of_sequence, 'Children (use in the order shown):', offset + 6)

    if e.children_of_choice is not None and len(e.children_of_choice) > 0:
        e.children_of_choice.sort(key=lambda x:x.name)
        print_children_html(e.children_of_choice, 'Children (use only one):', offset + 6)

    if len(e.children_of_all) > 0 or len(e.children_of_sequence) > 0 or len(e.children_of_choice) > 0 or len(e.attributes) > 0:
        print justify('</ul>', offset + 3)
        print justify('</li>', offset)

def print_all_html():
    elements = get_core()
    elements.sort(key=lambda x:x.name)
    print "<html>"
    print justify('<head>', 3)
    print justify('<link rel="stylesheet" type="text/css" href="xsd2html.css">', 6)
    print justify('<link href="http://static.jboss.org/css/rhbar.css" media="screen" rel="stylesheet">',6)
    print justify('<script type="text/javascript" src="xsd2html.js"></script>',6)
    print justify('</head>', 3)
    print justify("<body>", 3)
    print justify('<div id="rhbar"><a class="rhd-logo" href="http://developers.redhat.com"></a><a class="rhlogo" href="http://www.redhat.com/"></a></div>',6)
    print justify('<h2 align="middle">AMQ 7 Broker<br>Heirarchy of Configuration Elements</h2>', 6)
    print justify('<hr align="middle" width="50%"/>', 6)
    print justify('<br>The following list presents the configuration elements for AMQ 7 Broker in heirarchical order. The top level elements can appear in any order under the "&lt;core&gt;" tag in broker.xml. Specific usage requirements for their sub-elements are presented within the list.<br><br>',6)
    print justify('<div id="show-hide">',6)
    print justify('<input type="checkbox" checked onClick="togglecheckboxes()"/>', 9)
    print justify('Expand', 9)
    print justify('</div>',6)
    print justify('&lt;configuration&gt;<br>',0)
    print justify('&nbsp;&nbsp;&nbsp;&lt;core&gt;',0)
    print justify('<ul class="tree">', 6)
    for e in elements:
        print_element_html(e)
    print justify("</ul>", 6)
    print justify('<div class="container" id="companyfooter">', 6)
    print justify('<div class="redhatlogo" style="text-align:center;">', 6)
    print justify('<div id="logospacer"></div>', 6)
    print justify('<a href="http://www.redhat.com/">', 6)
    print justify('<img src="http://static.jboss.org/images/rhbar/redhatlogo.png">', 6)
    print justify('</a>', 6)
    print justify('</div>', 6)
    print justify('</div>', 6)
    print justify("</body>", 3)
    print "</html>"

def print_usage():
    print "Parses the file artemis-configuration.xsd and displays the information in the specified format: -a for asciidoc, -c for csv, -h for html. Use the '>' operator to write to a file."
    print 'USAGE: python xsdparser.py <-a|-c|-h> > <file>'

def main(argv):
    try:
        opts, args = getopt.getopt(argv,"ach",["asciidoc","csv","html"])
    except getopt.GetoptError:
        print_usage()
        sys.exit(2)
    if len(opts) < 1:
        print_usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ("-a", "--asciidoc"):
            print_all_adoc()
            sys.exit()
        elif opt in ("-c", "--csv"):
            print_all_csv()
            sys.exit()
        elif opt in ("-h", "--html"):
            print_all_html()
            sys.exit()

if __name__ == "__main__":
   main(sys.argv[1:])
