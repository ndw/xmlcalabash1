<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:px="http://xproc.dev.java.net/ns/extensions" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:try>
        <p:group>
          <p:output port="out" sequence="false"/>
          <p:identity/>
        </p:group>
        <p:catch>
          <p:output port="out" sequence="true"/>
          <p:identity/>
        </p:catch>
      </p:try>
    </p:pipeline>