<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:try>
        <p:group>
          <p:load href="i-do-not-exist.xml"/>
        </p:group>
        <p:catch>
          <p:identity/>
        </p:catch>
      </p:try>
    </p:pipeline>