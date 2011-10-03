<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      
      <p:parameters name="parameters">
        <p:with-param port="parameters" name="p:param" select="'value'">
          <p:empty/>
        </p:with-param>
      </p:parameters>

    </p:declare-step>