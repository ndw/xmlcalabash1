<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:param name="version.label"/>

  <xsl:template match="/">
    <installation version="1.0">

      <!--
          The info section.
          The meaning of the tags should be natural ...
      -->
      <info>
        <appname>XML Calabash</appname>
        <appversion><xsl:value-of select="$version.label"/></appversion>
        <appsubpath>xmlcalabash-<xsl:value-of select="$version.label"/></appsubpath>
        <authors>
          <author name="Norman Walsh" email="ndw@nwalsh.com"/>
        </authors>
        <url>http://xmlcalabash.com/</url>
        <javaversion>1.6</javaversion>
      </info>

      <!--
          The gui preferences indication.
          Sets the installer window to 640x480. It will not be able to change the size.
      -->
      <guiprefs width="640" height="480" resizable="no"/>

      <locale>
        <langpack iso3="eng"/>
      </locale>

      <!--
          The resources section.
          The ids must be these ones if you want to use the LicencePanel and/or
          the InfoPanel.
      -->
      <resources>
        <res id="LicencePanel.licence" src="../resources/notices/CDDL+GPL.txt"/>
      </resources>

      <!--
          The panels section.
          We indicate here which panels we want to use. The order will be respected.
      -->
      <panels>
        <panel classname="HelloPanel"/>
        <panel classname="LicencePanel"/>
        <panel classname="TargetPanel"/>
        <panel classname="PacksPanel"/>
        <panel classname="InstallPanel"/>
        <panel classname="FinishPanel"/>
      </panels>

      <!--
          The packs section.
          We specify here our packs.
      -->
      <packs>
        <pack name="Base" required="yes">
          <description>The base files</description>
          <fileset dir="calabash-{$version.label}" targetdir="$INSTALL_PATH"/>
          <parsable type="shell" targetfile="$INSTALL_PATH/calabash"/>
          <parsable type="shell" targetfile="$INSTALL_PATH/calabash.bat"/>
          <executable targetfile="$INSTALL_PATH/calabash" stage="never"/>
        </pack>
      </packs>
    </installation>
  </xsl:template>

</xsl:transform>
