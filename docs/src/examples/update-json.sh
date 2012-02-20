for flavor in "marklogic" "jsonx" "jxml" "calabash" "calabash-deprecated"
do
    echo "Converting JSON to $flavor..."
    calabash -Xtransparent-json -Xjson-flavor=$flavor -oresult=../results/json-$flavor.xml json.xpl
done
