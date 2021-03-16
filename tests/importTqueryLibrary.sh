rm -rf lib
rm -rf packages
mkdir lib
mkdir -p packages/tquery
ln -h ../build/libs/tquery.jar lib/tquery.jar 
ln -h ../packages/tquery/tquery.ol packages/tquery/tquery.ol
if ! command -v "jolie" &> /dev/null
then
    echo "Jolie could not be found. Please install it to run the tests."
    exit 1
fi
jolie tquery_client.ol