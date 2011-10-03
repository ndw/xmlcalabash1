<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">

      <p:viewport match="sub">
        <p:variable name="p1" select="p:iteration-position()"/>
        <p:variable name="s1" select="p:iteration-size()"/>

        <!-- a p:group wrapper to tests that p:iteration-position
             and p:iteration-size are not affected -->
        <p:group>
          <p:variable name="p2" select="p:iteration-position()"/>
          <p:variable name="s2" select="p:iteration-size()"/>

          <p:viewport match="para">
            <p:variable name="p3" select="p:iteration-position()"/>
            <p:variable name="s3" select="p:iteration-size()"/>
            
            <p:add-attribute match="item" attribute-name="pos">
              <p:input port="source">
                <p:inline>
                  <item/>
                </p:inline>
              </p:input>
              <p:with-option name="attribute-value" select="concat($p1, ',', $s1, '-', $p2, ',', $s2, '-', $p3, ',', $s3)"/>
            </p:add-attribute>
          </p:viewport>
        </p:group>
      </p:viewport>

    </p:pipeline>