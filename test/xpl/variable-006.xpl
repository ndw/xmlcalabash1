<p:pipeline xmlns:ix="http://www.innovimax.fr/xproc/ns" xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:group>
        <p:variable name="varName" select="0"/>
        <p:identity/>
      </p:group>
      <p:group>
        <p:variable name="varName" select="'not a duplicate'"/>
        <p:identity/>
      </p:group>
    </p:pipeline>