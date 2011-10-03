<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:ex="http://example.com/ex" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" exclude-inline-prefixes="ex">

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

  <!-- tricky. options are strings so 0 = "0" = true() -->
  <ex:ebv name="ebv1">
    <p:with-option name="value" select="0"/>
    <p:with-option name="test" select="'ebv1'"/>
  </ex:ebv>
  <ex:ebv name="ebv2">
    <p:with-option name="value" select="1"/>
    <p:with-option name="test" select="'ebv2'"/>
  </ex:ebv>
  <ex:ebv name="ebv3">
    <p:with-option name="value" select="-1"/>
    <p:with-option name="test" select="'ebv3'"/>
  </ex:ebv>
  <!-- tricky. options are strings so number('NaN') = string('NaN') = true() -->
  <ex:ebv name="ebv4">
    <p:with-option name="value" select="number('NaN')"/>
    <p:with-option name="test" select="'ebv4'"/>
  </ex:ebv>
  <ex:ebv name="ebv5">
    <p:with-option name="value" select="true()"/>
    <p:with-option name="test" select="'ebv5'"/>
  </ex:ebv>
  <!-- tricky. options are strings so false() = "false" = true() -->
  <ex:ebv name="ebv6">
    <p:with-option name="value" select="false()"/>
    <p:with-option name="test" select="'ebv6'"/>
  </ex:ebv>
  <ex:ebv name="ebv7">
    <p:with-option name="value" select="'true'"/>
    <p:with-option name="test" select="'ebv7'"/>
  </ex:ebv>
  <ex:ebv name="ebv8">
    <p:with-option name="value" select="'false'"/>
    <p:with-option name="test" select="'ebv8'"/>
  </ex:ebv>
  <!-- tricky. options are strings so //ex:div = "" = false() -->
  <ex:ebv name="ebv9">
    <p:with-option name="value" select="//ex:div">
      <p:inline>
        <ex:div/>
      </p:inline>
    </p:with-option>
    <p:with-option name="test" select="'ebv9'"/>
  </ex:ebv>
  <ex:ebv name="ebv10">
    <p:with-option name="value" select="''"/>
    <p:with-option name="test" select="'ebv10'"/>
  </ex:ebv>

  <p:wrap-sequence wrapper="true-results" name="true-results">
    <p:input port="source">
      <p:pipe step="ebv1" port="result"/>
      <p:pipe step="ebv2" port="result"/>
      <p:pipe step="ebv3" port="result"/>
      <p:pipe step="ebv4" port="result"/>
      <p:pipe step="ebv5" port="result"/>
      <p:pipe step="ebv6" port="result"/>
      <p:pipe step="ebv7" port="result"/>
      <p:pipe step="ebv8" port="result"/>
    </p:input>
  </p:wrap-sequence>

  <p:wrap-sequence wrapper="false-results" name="false-results">
    <p:input port="source">
      <p:pipe step="ebv9" port="result"/>
      <p:pipe step="ebv10" port="result"/>
    </p:input>
  </p:wrap-sequence>

  <p:wrap-sequence wrapper="results">
    <p:input port="source">
      <p:pipe step="true-results" port="result"/>
      <p:pipe step="false-results" port="result"/>
    </p:input>
  </p:wrap-sequence>

</p:declare-step>