package com.ddlab.tornado.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DialogSettings implements IDialogSettings {
  private String name;

  private Map<String, IDialogSettings> sections;

  private Map<String, String> items;

  private Map<String, String[]> arrayItems;

  private static final String TAG_SECTION = "section";

  private static final String TAG_NAME = "name";

  private static final String TAG_KEY = "key";

  private static final String TAG_VALUE = "value";

  private static final String TAG_LIST = "list";

  private static final String TAG_ITEM = "item";

  public DialogSettings(String sectionName) {
    name = sectionName;
    items = new LinkedHashMap<>();
    arrayItems = new LinkedHashMap<>();
    sections = new LinkedHashMap<>();
  }

  @Override
  public IDialogSettings addNewSection(String sectionName) {
    DialogSettings section = new DialogSettings(sectionName);
    addSection(section);
    return section;
  }

  @Override
  public void addSection(IDialogSettings section) {
    sections.put(section.getName(), section);
  }

  public void removeSection(IDialogSettings section) {
    if (sections.get(section.getName()) == section) {
      sections.remove(section.getName());
    }
  }

  public IDialogSettings removeSection(String sectionName) {
    return sections.remove(sectionName);
  }

  @Override
  public String get(String key) {
    return items.get(key);
  }

  @Override
  public String[] getArray(String key) {
    return arrayItems.get(key);
  }

  @Override
  public boolean getBoolean(String key) {
    return Boolean.parseBoolean(items.get(key));
  }

  @Override
  public double getDouble(String key) throws NumberFormatException {
    String setting = items.get(key);
    if (setting == null) {
      throw new NumberFormatException(
          "There is no setting associated with the key \""
              + key
              + "\""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    return Double.parseDouble(setting);
  }

  @Override
  public float getFloat(String key) throws NumberFormatException {
    String setting = items.get(key);
    if (setting == null) {
      throw new NumberFormatException(
          "There is no setting associated with the key \""
              + key
              + "\""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    return Float.parseFloat(setting);
  }

  @Override
  public int getInt(String key) throws NumberFormatException {
    String setting = items.get(key);
    if (setting == null) {
      // Integer.valueOf(null) will throw a NumberFormatException and
      // meet our spec, but this message is clearer.
      throw new NumberFormatException(
          "There is no setting associated with the key \""
              + key
              + "\""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    return Integer.parseInt(setting);
  }

  @Override
  public long getLong(String key) throws NumberFormatException {
    String setting = items.get(key);
    if (setting == null) {
      // new Long(null) will throw a NumberFormatException and meet our spec, but this message
      // is clearer.
      throw new NumberFormatException(
          "There is no setting associated with the key \""
              + key
              + "\""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    return Long.parseLong(setting);
  }

  @Override
  public String getName() {
    return name;
  }

  public static IDialogSettings getOrCreateSection(IDialogSettings settings, String sectionName) {
    IDialogSettings section = settings.getSection(sectionName);
    if (section == null) {
      section = settings.addNewSection(sectionName);
    }
    return section;
  }

  @Override
  public IDialogSettings getSection(String sectionName) {
    return sections.get(sectionName);
  }

  @Override
  public IDialogSettings[] getSections() {
    Collection<IDialogSettings> values = sections.values();
    DialogSettings[] result = new DialogSettings[values.size()];
    values.toArray(result);
    return result;
  }

  @Override
  public void load(Reader r) {
    Document document = null;
    try {
      DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      //		parser.setProcessNamespace(true);
      document = parser.parse(new InputSource(r));

      // Strip out any comments first
      Node root = document.getFirstChild();
      while (root.getNodeType() == Node.COMMENT_NODE) {
        document.removeChild(root);
        root = document.getFirstChild();
      }
      load(document, (Element) root);
    } catch (ParserConfigurationException | IOException | SAXException e) {
      // ignore
    }
  }

  @Override
  public void load(String fileName) throws IOException {
    FileInputStream stream = new FileInputStream(fileName);
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      load(reader);
    }
  }

  private void load(Document document, Element root) {
    name = root.getAttribute(TAG_NAME);
    NodeList l = root.getElementsByTagName(TAG_ITEM);
    for (int i = 0; i < l.getLength(); i++) {
      Node n = l.item(i);
      if (root == n.getParentNode()) {
        String key = ((Element) l.item(i)).getAttribute(TAG_KEY);
        String value = ((Element) l.item(i)).getAttribute(TAG_VALUE);
        items.put(key, value);
      }
    }
    l = root.getElementsByTagName(TAG_LIST);
    for (int i = 0; i < l.getLength(); i++) {
      Node n = l.item(i);
      if (root == n.getParentNode()) {
        Element child = (Element) l.item(i);
        String key = child.getAttribute(TAG_KEY);
        NodeList list = child.getElementsByTagName(TAG_ITEM);
        List<String> valueList = new ArrayList<>();
        for (int j = 0; j < list.getLength(); j++) {
          Element node = (Element) list.item(j);
          if (child == node.getParentNode()) {
            valueList.add(node.getAttribute(TAG_VALUE));
          }
        }
        String[] value = new String[valueList.size()];
        valueList.toArray(value);
        arrayItems.put(key, value);
      }
    }
    l = root.getElementsByTagName(TAG_SECTION);
    for (int i = 0; i < l.getLength(); i++) {
      Node n = l.item(i);
      if (root == n.getParentNode()) {
        DialogSettings s = new DialogSettings("NoName"); // $NON-NLS-1$
        s.load(document, (Element) n);
        addSection(s);
      }
    }
  }

  @Override
  public void put(String key, String[] value) {
    if (value == null) {
      arrayItems.remove(key);
    } else {
      arrayItems.put(key, value);
    }
  }

  @Override
  public void put(String key, double value) {
    put(key, String.valueOf(value));
  }

  @Override
  public void put(String key, float value) {
    put(key, String.valueOf(value));
  }

  @Override
  public void put(String key, int value) {
    put(key, String.valueOf(value));
  }

  @Override
  public void put(String key, long value) {
    put(key, String.valueOf(value));
  }

  @Override
  public void put(String key, String value) {
    if (value == null) {
      items.remove(key);
    } else {
      items.put(key, value);
    }
  }

  @Override
  public void put(String key, boolean value) {
    put(key, String.valueOf(value));
  }

  @Override
  public void save(Writer writer) throws IOException {
    try (XMLWriter xmlWriter = new XMLWriter(writer)) {
      save(xmlWriter);
      xmlWriter.flush();
    }
  }

  @Override
  public void save(String fileName) throws IOException {
    try (XMLWriter writer = new XMLWriter(new FileOutputStream(fileName))) {
      save(writer);
    }
  }

  private void save(XMLWriter out) throws IOException {
    Map<String, String> attributes = new LinkedHashMap<>(2);
    attributes.put(TAG_NAME, name == null ? "" : name); // $NON-NLS-1$
    out.startTag(TAG_SECTION, attributes);
    attributes.clear();

    for (Entry<String, String> entry : items.entrySet()) {
      String key = entry.getKey();
      attributes.put(TAG_KEY, key == null ? "" : key); // $NON-NLS-1$
      String string = entry.getValue();
      attributes.put(TAG_VALUE, string == null ? "" : string); // $NON-NLS-1$
      out.printTag(TAG_ITEM, attributes, true);
    }

    attributes.clear();
    for (Entry<String, String[]> entry : arrayItems.entrySet()) {
      String key = entry.getKey();
      attributes.put(TAG_KEY, key == null ? "" : key); // $NON-NLS-1$
      out.startTag(TAG_LIST, attributes);
      String[] value = entry.getValue();
      attributes.clear();
      if (value != null) {
        for (String string : value) {
          attributes.put(TAG_VALUE, string == null ? "" : string); // $NON-NLS-1$
          out.printTag(TAG_ITEM, attributes, true);
        }
      }
      out.endTag(TAG_LIST);
      attributes.clear();
    }
    for (IDialogSettings iDialogSettings : sections.values()) {
      ((DialogSettings) iDialogSettings).save(out);
    }
    out.endTag(TAG_SECTION);
  }

  private static class XMLWriter extends BufferedWriter {

    /** current number of tabs to use for indent */
    protected int tab;

    /** the xml header */
    protected static final String XML_VERSION =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"; //$NON-NLS-1$

    public XMLWriter(OutputStream output) throws IOException {
      this(new OutputStreamWriter(output, StandardCharsets.UTF_8));
    }

    /**
     * Create a new XMLWriter
     *
     * @param output the write to used when writing to
     * @throws IOException
     */
    public XMLWriter(Writer output) throws IOException {
      super(output);
      tab = 0;
      writeln(XML_VERSION);
    }

    private void writeln(String text) throws IOException {
      write(text);
      newLine();
    }

    public void endTag(String name) throws IOException {
      tab--;
      printTag("/" + name, null, false); // $NON-NLS-1$
    }

    private void printTabulation() throws IOException {
      for (int i = 0; i < tab; i++) {
        super.write('\t');
      }
    }

    public void printTag(String name, Map<String, String> parameters, boolean close)
        throws IOException {
      printTag(name, parameters, true, true, close);
    }

    private void printTag(
        String name,
        Map<String, String> parameters,
        boolean shouldTab,
        boolean newLine,
        boolean close)
        throws IOException {
      StringBuilder sb = new StringBuilder();
      sb.append('<');
      sb.append(name);
      if (parameters != null) {
        for (Entry<String, String> entry : parameters.entrySet()) {
          sb.append(" "); // $NON-NLS-1$
          String key = entry.getKey();
          sb.append(key);
          sb.append("=\""); // $NON-NLS-1$
          sb.append(getEscaped(String.valueOf(entry.getValue())));
          sb.append("\""); // $NON-NLS-1$
        }
      }
      if (close) {
        sb.append('/');
      }
      sb.append('>');
      if (shouldTab) {
        printTabulation();
      }
      if (newLine) {
        writeln(sb.toString());
      } else {
        write(sb.toString());
      }
    }

    public void startTag(String name, Map<String, String> parameters) throws IOException {
      startTag(name, parameters, true);
      tab++;
    }

    private void startTag(String name, Map<String, String> parameters, boolean newLine)
        throws IOException {
      printTag(name, parameters, true, newLine, false);
    }

    private static void appendEscapedChar(StringBuilder buffer, char c) {
      String replacement = getReplacement(c);
      if (replacement != null) {
        buffer.append('&');
        buffer.append(replacement);
        buffer.append(';');
      } else {
        buffer.append(c);
      }
    }

    private static String getEscaped(String s) {
      StringBuilder result = new StringBuilder(s.length() + 10);
      for (int i = 0; i < s.length(); ++i) {
        appendEscapedChar(result, s.charAt(i));
      }
      return result.toString();
    }

    private static String getReplacement(char c) {
      // Encode special XML characters into the equivalent character references.
      // The first five are defined by default for all XML documents.
      // The next three (#xD, #xA, #x9) are encoded to avoid them
      // being converted to spaces on deserialization
      switch (c) {
        case '<':
          return "lt"; //$NON-NLS-1$
        case '>':
          return "gt"; //$NON-NLS-1$
        case '"':
          return "quot"; //$NON-NLS-1$
        case '\'':
          return "apos"; //$NON-NLS-1$
        case '&':
          return "amp"; //$NON-NLS-1$
        case '\r':
          return "#x0D"; //$NON-NLS-1$
        case '\n':
          return "#x0A"; //$NON-NLS-1$
        case '\u0009':
          return "#x09"; //$NON-NLS-1$
      }
      return null;
    }
  }
}
