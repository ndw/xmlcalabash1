<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:viewport match="para" name="viewport">

        <p:choose name="choose">
          <p:when test="p:iteration-position() = 1">
            <p:identity>
              <p:input port="source">
                <p:inline><first/></p:inline>
                <p:pipe step="viewport" port="current"/>
              </p:input>
            </p:identity>
          </p:when>
          <p:when test="p:iteration-position() = p:iteration-size()">
            <p:identity>
              <p:input port="source">
                <p:pipe step="viewport" port="current"/>
                <p:inline><last/></p:inline>
              </p:input>
            </p:identity>
          </p:when>
          <p:otherwise>
            <p:identity/>
          </p:otherwise>
        </p:choose>

      </p:viewport>

    </p:pipeline>