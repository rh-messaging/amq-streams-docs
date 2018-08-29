# Establish global variables to the docs and script dirs
CURRENT_DIR="$( pwd -P)"
SCRIPT_SRC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd -P )"
DOCS_SRC="$( dirname $SCRIPT_SRC )"
BUILD_RESULTS="Build Results:"
BUILD_MESSAGE=$BUILD_RESULTS
BLACK='\033[0;30m'
RED='\033[0;31m'
NO_COLOR="\033[0m"
# Do not produce pot/po by default
L10N=
# A comma separated lists of default locales that books should be translated to.
# This can be overriden in each book's individual buildGuide.sh
LANG_CODE=ja-JP

usage(){
  cat <<EOM
USAGE: $0 [OPTION]... <guide>

DESCRIPTION: Build all of the guides (default), a single guide, and (optionally)
produce pot/po files for translation.

Run this script from either the root of your cloned repo or from the 'scripts'
directory.  Example:
  cd eap-documentation/scripts
  $0

OPTIONS:
  -h       Print help.
  -t       Produce the pot/po files for the designtated guide(s)

EXAMPLES:
  Build all guides:
   $0

  Build all guides and produce pot/po:
   $0 -t

  Build a specific guide(s) from $DOCS_SRC:
    $0 cli-guide
    $0 config-guide
    $0 config-guide deploying-on-amazon-ec2

  Build a specific guide from $DOCS_SRC with pot/po:
   $0 -t cli-guide
   $0 -t config-guide
   $0 -t config-guide deploying-on-amazon-ec2
EOM
}

OPTIND=1
while getopts "ht" c
 do
     case "$c" in
       t)         L10N="-t $LANG_CODE";;
       h)         usage
                  exit 1;;
       \?)        echo "Unknown option: -$OPTARG." >&2
                  usage
                  exit 1;;
     esac
 done
shift $(($OPTIND - 1))

# Use $DOCS_SRC so we don't have to worry about relative paths.
cd $DOCS_SRC

# Set the list of docs to build to whatever the user passed in (if anyting)
subdirs=$@
if [ $# -gt 0 ]; then
  echo "=== Bulding $@ ==="
else
  echo "=== Building all the guides ==="
  # Recurse through the guide directories and build them.
  subdirs=`find . -maxdepth 1 -type d ! -iname ".*" ! -iname "topics" | sort`
fi
echo $PWD
for subdir in $subdirs
do
  echo "Building $DOCS_SRC/${subdir##*/}"
  # Navigate to the dirctory and build it
  if ! [ -e $DOCS_SRC/${subdir##*/} ]; then
    BUILD_MESSAGE="$BUILD_MESSAGE\nERROR: $DOCS_SRC/${subdir##*/} does not exist."
    continue
  fi
  cd $DOCS_SRC/${subdir##*/}
  GUIDE_NAME=${PWD##*/}
  if [ -e ./buildGuide.sh ]; then
    ./buildGuide.sh $L10N
    if [ "$?" = "1" ]; then
      BUILD_ERROR="ERROR: Build of $GUIDE_NAME failed. See the log above for details."
      BUILD_MESSAGE="$BUILD_MESSAGE\n$BUILD_ERROR"
    fi
  fi
  # Return to the parent directory
  cd $SCRIPT_SRC
done

# Return to where we started as a courtesy.
cd $CURRENT_DIR

# Report any errors
echo ""
if [ "$BUILD_MESSAGE" == "$BUILD_RESULTS" ]; then
  echo "Build was successful!"
else
  echo -e "${RED}$BUILD_MESSAGE${NO_COLOR}"
  echo -e "${RED}Please fix all issues before requesting a merge!${NO_COLOR}"
fi
exit;
