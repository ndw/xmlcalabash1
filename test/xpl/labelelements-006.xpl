<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

<p:documentation>
Note: this test relies on the fact that it's statistically unlikely that
generated ID values for any given implementation will begin with the
string ‘START'.
</p:documentation>

<p:label-elements replace="false"/>

<p:for-each>
  <p:output port="result"/>
  <p:iteration-source select="//*[@xml:id and not(starts-with(@xml:id,'START'))]"/>
  <p:identity/>
</p:for-each>

<p:count/>

</p:pipeline>