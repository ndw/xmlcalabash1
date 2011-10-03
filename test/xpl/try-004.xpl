<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
<p:input port="source"/>
<p:output port="result"/>
    <p:try name="try">
      <p:group>
        <p:output port="result"/>

        <p:try name="try-nested">
            <p:group>
                <p:compare fail-if-not-equal="true">
                    <p:input port="alternate">
                        <p:inline><doc1/></p:inline>
                    </p:input>
                </p:compare>
                <p:identity>
                    <p:input port="source">
                        <p:inline>
                            <p>p:compare succeeded</p>
                        </p:inline>
                    </p:input>
                </p:identity>
            </p:group>
              <p:catch>
                <p:identity>
                  <p:input port="source">
                    <p:inline><p>p:compare failed at the nested level</p></p:inline>
                  </p:input>
                </p:identity>
              </p:catch>
        </p:try>
      </p:group>
      <p:catch>
        <p:output port="result"/>
        <p:identity>
          <p:input port="source">
            <p:inline><p>p:compare failed at the top level</p></p:inline>
          </p:input>
        </p:identity>
      </p:catch>
    </p:try>
</p:declare-step>