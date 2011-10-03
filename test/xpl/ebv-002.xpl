<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:ex="http://example.com/ex" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" exclude-inline-prefixes="ex" xpath-version="2.0">

  <p:output port="result"/>

  <p:declare-step type="ex:ebv">
    <p:output port="result"/>
    <p:option name="value" required="true"/>
    <p:option name="test" required="true"/>

    <p:try>
      <p:group>
        <p:choose>
          <p:when test="$value">
            <p:identity>
              <p:input port="source">
                <p:inline>
                  <true/>
                </p:inline>
              </p:input>
            </p:identity>
          </p:when>
          <p:otherwise>
            <p:identity>
              <p:input port="source">
                <p:inline>
                  <false/>
                </p:inline>
              </p:input>
            </p:identity>
          </p:otherwise>
        </p:choose>
      </p:group>
      <p:catch>
        <p:identity>
          <p:input port="source">
            <p:inline>
              <error/>
            </p:inline>
          </p:input>
        </p:identity>
      </p:catch>
    </p:try>

    <p:add-attribute attribute-name="test" match="/*">
      <p:with-option name="attribute-value" select="$test"/>
    </p:add-attribute>
    <p:add-attribute attribute-name="expr" match="/*">
      <p:with-option name="attribute-value" select="$value"/>
    </p:add-attribute>
  </p:declare-step>

  <ex:ebv name="ebv1">
    <p:with-option name="value" select="(1,2)"/>
    <p:with-option name="test" select="'ebv1'"/>
  </ex:ebv>

  <ex:ebv name="ebv2">
    <p:with-option name="value" select="xs:dateTime('2011-09-01T08:35:00-04:00')"/>
    <p:with-option name="test" select="'ebv2'"/>
  </ex:ebv>

  <p:wrap-sequence wrapper="true-results" name="true-results">
    <p:input port="source">
      <p:pipe step="ebv1" port="result"/>
      <p:pipe step="ebv2" port="result"/>
    </p:input>
  </p:wrap-sequence>

  <p:wrap-sequence wrapper="results">
    <p:input port="source">
      <p:pipe step="true-results" port="result"/>
    </p:input>
  </p:wrap-sequence>

</p:declare-step>