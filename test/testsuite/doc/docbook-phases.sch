<?xml version="1.0" encoding="utf-8"?>
<s:schema xmlns:s="http://purl.oclc.org/dsdl/schematron"
          xmlns:db="http://docbook.org/ns/docbook"
          defaultPhase="full">
   <s:ns prefix="db" uri="http://docbook.org/ns/docbook"/>

   <s:phase id="full">
     <s:active pattern="root-has-version"/>
     <s:active pattern="biblioref-constraint"/>
   </s:phase>

   <s:phase id="no-biblioref-checking">
     <s:active pattern="root-has-version"/>
   </s:phase>

   <s:pattern name="Root must have version" id="root-has-version">
      <s:rule context="/db:para">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:set">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:book">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:dedication">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:acknowledgements">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:colophon">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:appendix">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:chapter">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:part">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:preface">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:section">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:article">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:sect1">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:sect2">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:sect3">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:sect4">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:sect5">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:reference">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:refentry">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:refsection">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:refsect1">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:refsect2">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:refsect3">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:glossary">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:bibliography">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:index">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:setindex">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
      <s:rule context="/db:toc">
         <s:assert test="@version">The root element must have a version attribute.</s:assert>
      </s:rule>
   </s:pattern>

   <s:pattern name="'biblioref' type constraint" id="biblioref-constraint">
      <s:rule context="db:biblioref[@linkend]">
         <s:assert test="local-name(//*[@xml:id=current()/@linkend]) = 'bibliomixed' and namespace-uri(//*[@xml:id=current()/@linkend]) = 'http://docbook.org/ns/docbook'">@linkend on biblioref must point to a bibliography entry.</s:assert>
      </s:rule>
   </s:pattern>
</s:schema>
