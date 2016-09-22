# Universal Dependencies to TüNDRA XML Converter

This ReadMe is by no means complete, but it can help you to get started with the converter.
TüNDRA is an online treebank research tool available at https://weblicht.sfs.uni-tuebingen.de/Tundra/
In order to access the treebanks, you need to log in with your university account. TüNDRA stores the treebank data in its own XML-based format. This repository contains only publicly available treebanks taken from open resources and converted into the above mentioned format. Along with the XML files we put the converter, so you could adjust it to your needs. 

## Usage

First, you may want to build a package using Maven:
```
mvn clean package
```

Now you can run the jar file from the command line (it is necessary to specify the class):
```
java -cp tundra-converters-1.0.2-SNAPSHOT.jar de.tuebingen.uni.sfs.clarin.tundra.udep.UniversalDependency UD_TREEBANKS_FOLDER/
```

XML files will be saved at the same folder where the initial files are located (nothing is removed). The converter merges all *.CoNNLU files belonging to a single treebank and generates one XML file.

## Treebank installation

1. TüNDRA is using a BaseX database (http://basex.org/). This package has to be installed
2. Run the following command to add a specific XML treebank to the BaseX storage```basex -c"create database DATABASE_NAME XML_FILE; open DATABASE_NAME; create index attribute"```
3. BaseX usually stores its databases at the folder called BaseXData (which is created at the home folder). If there is no such a folder, you should create it manually
4. Each treebank must have a metadata file (located at the BaseXData folder). It is easier to create one using an exisiting metadata file. The next section decribes this process
5. Restart TüNDRA

The repository includes a Bash script ```makebasex.sh``` that automatically executes the command line utility (the basex command from the above) for each XML file located at the current directory. The script saves a lot of time when you have more than 10 treebanks to process. Names for the treebanks are assigned based on the name of corresponding XML files.

## Metadata file

Here is an example of a metadata file for the UD Arabic treebank:

```
<metadata>
  <longname>The Arabic UD treebank</longname>
  <shortname>UD_Arabic</shortname>
  <description>The Arabic UD treebank is based on the Prague Arabic Dependency Treebank (PADT), created at the Charles University in Prague.</description>
  <tkpage>http://universaldependencies.org/format.html</tkpage>
  <infopage>http://ufal.mff.cuni.cz/padt/</infopage>
  <icon>http://4.bp.blogspot.com/_n9xsNvtavBs/S6ZGQsEd6NI/AAAAAAAAAAg/zrTMU7kIvXw/S226/PADT-logo.gif</icon>
  <font>Arial</font>
  <fallback>Sans</fallback>
  <publish>true</publish>
  <language>Arabic</language>
  <rtl>true</rtl>
  <license>UD licence</license>
  <version>1.0</version>
  <visible>true</visible>
  <annotation>UD annotated</annotation>
  <abbreviations>
    <abbreviation>
      <attribute>lemma</attribute>
      <description>Lemma</description>
    </abbreviation>
  </abbreviations>
  <showAttributes>
    <attribute>lemma</attribute>
    <attribute>pos</attribute>
    <attribute>xpos</attribute>
    <attribute>Case</attribute>
    <attribute>Definite</attribute>
    <attribute>Number</attribute>
    <attribute>Aspect</attribute>
    <attribute>Gender</attribute>
    <attribute>Mood</attribute>
    <attribute>Person</attribute>
    <attribute>VerbForm</attribute>
    <attribute>Voice</attribute>
    <attribute>AdpType</attribute>
    <attribute>NumForm</attribute>
    <attribute>Abbr</attribute>
    <attribute>Foreign</attribute>
  </showAttributes>
  <databases>
        <database>UD_Arabic</database>
  </databases>
</metadata>
```

### Node description
* longname - name that appears on the list of all treebanks available at the system
* shortname - unique identifier used by the system (no spaces allowed)
* description - short desciption displayed on the main page
* tkpage - link to the page describing the tag set
* infopage - link to the page where the user can find more information about a specific treebank
* icon - link to the icon
* font - font that has to be used (for now ignored by the system)
* fallback - second font (if the first one cannot be applied)
* publish - display on the list of all available treebanks (true/false)
* language - treebank language
* rtl - right-to-left language (true/false)
* license - treebank license
* version - treebank version
* visible - treebank is publicly available and can be accessed without logging in (true/false)
* annotation - information about the annotation
* abbreviations - abbreviations for the displayed attributes
* showAttributes - attributes that should be displayed under sentence tokens

## TüNDRA XML format

At the moment TüNDRA does not have any fixed XML schema for its treebanks. However, you should avoid using the following attributes:

1. num - number of a node
2. start - minimal value of the "order" attribute (among all children of a given node)
3. finish - maximal value of the "order" attribute (among all children of a given node)
4. order - position index of a token in a sentence

## Credits

TüNDRA (https://weblicht.sfs.uni-tuebingen.de/Tundra/) is a free treebank research tool supported by the CLARIN-D (http://de.clarin.eu/) project and Seminar für Sprachwissenschaft (http://www.sfs.uni-tuebingen.de/) at Eberhard Karls Universität Tübingen (http://www.uni-tuebingen.de/).


## License

All the treebanks available here are public and provided as they are (nothing has been changed, except for the format). Original files were taken from http://universaldependencies.org. Please consult this web page, if you have any questions concerning licenses.
