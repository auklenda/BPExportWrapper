package bpexportwrapper;
import com.sterlingcommerce.woodstock.packager.MIME.EncoderDecoder;
import java.util.HashMap;
import java.util.ArrayList;
import com.sterlingcommerce.woodstock.profile.ie.resource.bpdef.BPDefExporter;
//import com.sterlingcommerce.woodstock.profile.ie.resource.bpdef.ExportTarget;
import com.sterlingcommerce.woodstock.profile.ie.*;
import com.sterlingcommerce.woodstock.ui.BPFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import aea.auklend.AALogger;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import javax.xml.transform.OutputKeys;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NamedNodeMap;
/**
 *
 * @author Alf E. Auklend
 */
public class BPExportWrapper {
    private String cls = BPExportWrapper.class.getName();
    String bpNameLike = null;
    String strAction = "export";
    String bpFileNameList= null;
    int nAction = 1;
    boolean allBPs = false;
    boolean IMPRT = false;
    boolean keepInputXML = false;
    boolean dropSI = false;
 //   private BPDefExporter bpde = null;
//    private ExportTarget et = null; 
    //private IEReport exportReport = null;
    //OutputStream outstream = null;
    InputStream ins = null;
    String xmlFname = null;
    String dirPath = null;    // workdirectory
    String siPath = null;     // path to SI directory
    String dfaultFN = "./";
    String closeTag = "</SI_RESOURCES>";
    long byteLength = 0l;
    public static boolean debug = false;
    //Testinvocation :
    //-action wfusage -bp AAimportTest -dbg -dirpath C:\Customers\SocGen\ETECE\ExportImport\exported-files\
    //-action gpm -bp AAimportTest -dbg -dirpath C:\Customers\SocGen\ETECE\ExportImport\exported-files\
    //-action export -bp AAimportTest -dbg -dirpath C:\Customers\SocGen\ETECE\ExportImport\exported-files\
    //-action addwf -bp AAimportTest -dbg -dirpath C:\Customers\SocGen\ETECE\ExportImport\exported-files\
    private boolean checkUTF8 = true;
    

    public BPExportWrapper() {
    }
    public long openInput(String fileName) {
        long bytes = 0l;
        try {
            bytes = new File(fileName).length();
            ins = new FileInputStream(fileName);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            AALogger.logError(cls, "FileNotFoundException :" + fileName );
            return -1;
        }
        return bytes;
    }
    /** 
     * This will create GITHUB files based on a list of WorkFlow (BP) names
     * The BP names are listed in a the file read by this method
     * @param fn 
     */
    public ArrayList readBPlistFile(String fn) {
        InputStream in; //=null;
        ArrayList bpNames = new ArrayList();
        if (bpNameLike != null) {
            bpNames.add(bpNameLike);
        } else {
            try {
                in = new FileInputStream(new File(fn));
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line; 
                while((line = br.readLine()) != null) {        
                    bpNames.add(line);
                }
                in.close();
            } catch (Exception ex) {
                ex.getStackTrace();
                AALogger.logError(cls, "Exception :" + ex.getMessage() );
                return null;
            } 
        }
        return bpNames;
    }
    //
    // Make sure path name ends with a file seperator character
    //
    public String fixDirName(String dir){
        String ret = dir;
        StringBuilder sb = new StringBuilder();
        if (dir == null)
           sb.append(".").append(File.separator);
        else if (!(dir.endsWith(File.separator))) {
                sb.append(dir).append(File.separator);
                ret = sb.toString();
        }        
       return ret;
    }
    /*
    * String to intreger table. Action string from commandLine invocation
    */
    public void setAction(String action){
        if (action.equalsIgnoreCase("export"))
            nAction = 1;
        else if ((action.equalsIgnoreCase("addwf")))
            nAction = 2;
        else if ((action.equalsIgnoreCase("gpm")))
            nAction = 3;
        else if ((action.equalsIgnoreCase("wfusage")))
            nAction = 4;
    }
    public String decode(String dataIn){
        byte [] data = dataIn.getBytes();
        int len = data.length;
        EncoderDecoder ecdc = new EncoderDecoder();
        //SIbase64encodedecode ecdc = new SIbase64encodedecode();
        byte [] ddata = new byte[data.length];
        int resultLength = ecdc.base64DecodeLine(data, ddata, len);
        String outdata = new String(ddata,0,resultLength);
        return outdata;
    }
    /**
     * 
     * @param d
     * @return 
     * @throws Exception
     * The Document d contain the xml produced fro SI export
     * and d (document) can contain multiple BPs (wild cards)
     * copyNode will copy individual BPs into a new document (newdoc)
     */
    public Document copyNode(Document d) throws Exception {
        Document newdoc=null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            newdoc = db.newDocument();
            // create a root node in newdoc
	    Element ele = newdoc.createElement("SI_RESOURCES");
	    newdoc.appendChild(ele);
            // Allways copy the root node from the SI exported xml   
            NodeList nl1 = d.getElementsByTagName("SI_RESOURCES");
            Node n1 = nl1.item(0);  // get root from export XML
            NamedNodeMap nmm = n1.getAttributes();
            int lnmm =nmm.getLength();
            for (int i = 0; i < nmm.getLength(); i++) {
                Node n = nmm.item(i); 
                String name = n.getNodeName();
                String val = n.getNodeValue();
                ele.setAttribute(name, val);
                //System.out.println("Name Map: " + name + "  value: " +  val );
            }
            Element ele1 = newdoc.createElement("BPDEFS");
            ele.appendChild(ele1);
            NodeList list = d.getElementsByTagName("BPDEF");
            Element element = (Element) list.item(0);           
            // Imports a node from another d document to this document, without altering
            // or removing the source node from the original document
            Node copiedNode = newdoc.importNode(element, true);          
            // Adds the node to the end of the list of children of this node
            //doc.getDocumentElement().appendChild(copiedNode);
            ele1.appendChild(copiedNode);
            //System.out.println("After Copy...");
            if (debug)
                prettyPrint(newdoc);
            NodeList nlDoc = d.getElementsByTagName("BPDEFS");
            Element eleDoc = (Element)nlDoc.item(0);
            NodeList nlList = d.getElementsByTagName("BPDEF");
            Element el1 = (Element) nlList.item(0);
            eleDoc.removeChild((Node)el1);
        } catch (Exception exp) {
            exp.printStackTrace();
            AALogger.logError(cls, "Could not create document for BP");   
            return null;
        }    
	return newdoc;	
    }
     public static final void prettyPrint(Document xml) throws Exception {
	Transformer tf = TransformerFactory.newInstance().newTransformer();
	tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	tf.setOutputProperty(OutputKeys.INDENT, "yes");
	Writer out = new StringWriter();
	tf.transform(new DOMSource(xml), new StreamResult(out));
	System.out.println(out.toString());
    }
    /*
     public void nodesToCopy(Document d) {
        NodeList nToCopy = d.getElementsByTagName("BPDEF");
        Document newDoc = null;
        for(Node n : nToCopy) {
    // Create a duplicate node
            Node newNode = n.cloneNode(true);
    // Transfer ownership of the new node into the destination document
             newDoc.adoptNode(newNode);
    // Make the new node an actual item in the target document
            newDoc.getDocumentElement().appendChild(newNode);
         }        
    }
    */
    /**
    * Step1 - Export the BP from SBI based on the BP name
    * Result in an XML file with the namer [BPname].xml
    */
    public boolean exportBP(String bpName) {
        //fixDirName();
        StringBuilder sb = new StringBuilder();
        sb.append(dirPath).append(bpName);
        AALogger.logInfo(cls,"Start of export " + sb.toString() + ".xml"); 
        sb.append("_exported.xml");
        String fne = sb.toString();
        AALogger.logDebug(cls, "Export file: " + fne);
        OutputStream outstream = null;
        try {
            outstream = new FileOutputStream(fne);
       } catch (FileNotFoundException ex) {
            AALogger.logError(cls,  " Could not get outputstream for : " + fne); 
            return false;
       }
       AALogger.logDebug(cls,"Start of export " + fne);  
       BPDefExporter bpde = new BPDefExporter();
       //ExportTarget et = new ExportXML(new ByteArrayOutputStream());
       ExportTarget et = new ExportXML(outstream);
       IEReport exportReport = new IEReport("exportExport");
       bpde.setIEReport(exportReport);       
       HashMap bps = new HashMap();
       ArrayList al = bpde.getAvailableResourceNames(bpName);
       bps.put("BPDEFS", al);
       int status = bpde.exportResources(et, bps, false);
       if (!(bpde.getObjectStatus())) {
          AALogger.logError(cls, "Failed to export " + exportReport.getAsString());   
          return false;
       }
       try {
           outstream.write(closeTag.getBytes());
           outstream.close();
       } catch (IOException ex) {
           ex.printStackTrace();
           AALogger.logError(cls, "Could not close file " + fne);  
            //Logger.getLogger(BPExportWrapper.class.getName()).log(Level.SEVERE, null, ex);
           return false;
       }
       return true; 
    }
    /**
     * 
     * @param fileName
     * @return 
     */
    public Document loadDocument(String fileName){
       DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
       DocumentBuilder dBuilder =null;
       try {
           dBuilder = dbFactory.newDocumentBuilder();
       } catch (ParserConfigurationException ex) {
           ex.printStackTrace();
           AALogger.logError(cls, "ParserConfigurationException :" + ex.getMessage());  
           return null;
       }
       Document doc = null;
       File inputFile = new File(fileName);
       try {
           doc = dBuilder.parse(inputFile);
       } catch (SAXException | IOException ex) {
           ex.printStackTrace();
           AALogger.logError(cls, ex.getMessage());  
       }
       if (!keepInputXML)                 // Fix to analyze export file from SI
           inputFile.delete();
       return doc;        
    }
    /**
     * loadDom- step2
     * read exported xml and insert the clear text BPML node via DOM 
     * @param fileName
     * @param tag
     * @throws TransformerConfigurationException 
     */
   public boolean loadDOM(String filePath, String tag,String bp) throws TransformerConfigurationException, Exception{ 
    //String fileName = filePath + "_exported.xml";
    String fileName = filePath + bp +"_exported.xml"; 
    String clearText = null;
    int nonUTF8 = 0;
    AALogger.logDebug(cls, "Export File :" + fileName + "  tag: " + tag);  
    Document doc = loadDocument(fileName);
    if (doc == null) {
        AALogger.logError(cls, "Could not create DOM for BP :" + bp );
        return false;
    }
    NodeList nlList = doc.getElementsByTagName(tag);
    int lng = nlList.getLength();
    AALogger.logInfo(cls,"Number of BPs in " + fileName + ": " + lng);
    //Element elt1 = doc.getDocumentElement();
    for (int ii = 0; ii < lng; ii++) {
       Document newdoc = copyNode(doc);
       if (newdoc == null)                  // newdoc is null if copyNode took an exception
           continue;                        // we should be able to continue to iterate 
       String bp_name = null;
       NodeList nList = newdoc.getElementsByTagName(tag);
       for (int temp = 0; temp < nList.getLength(); temp++) {
           Node nNode = nList.item(temp);
           if (nNode.getNodeType() == Node.ELEMENT_NODE) {
               Element eElement = (Element) nNode;
               bp_name = eElement.getElementsByTagName("ConfProcessName").item(0).getTextContent();
               String encode64bpml = eElement.getElementsByTagName("LangResource").item(0).getTextContent();
               clearText = decode(encode64bpml.substring(11));
               if (checkUTF8) {
                   for (int i = 0; i < clearText.length(); i++) {
                        if (clearText.charAt(i) > 0x7f) 
                           nonUTF8++;                           
                   }
               }
               // Include the clear text BPML in DOM
               Element newItem = newdoc.createElement("ClearBPML");  // Create BPML tag
               CDATASection cdnode = newdoc.createCDATASection(clearText);  // Create CDATASection Node for the bpml
               newItem.appendChild(cdnode);   // build node
               Node pos = nNode.getFirstChild().getNextSibling();  // Place after <LangResource>
               eElement.insertBefore(newItem, pos);  // Insert bpml (CDATA) in DOM    
           }
       }
       // Use a Transformer for output
       //String outXML = filePath + ".xml";
       String outXML = filePath + bp_name + ".xml";
       //String outXML = fileName;
       OutputStream xmlstream = null;
       try {
           xmlstream = new FileOutputStream(outXML);
       } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            AALogger.logError(cls, ex.getMessage());  
       }       
       TransformerFactory tFactory =  TransformerFactory.newInstance();
       Transformer transformer = tFactory.newTransformer();
       //DOMSource source = new DOMSource(doc);
       DOMSource source = new DOMSource(newdoc);
       StreamResult result = new StreamResult(xmlstream);
       try {
            transformer.transform(source, result);
       } catch (TransformerException ex) {
           ex.printStackTrace();
           AALogger.logError(cls, ex.getMessage());
       } catch (Exception ex) {
           ex.printStackTrace();
           AALogger.logError(cls, ex.getMessage());           
       }
       try {
           if (xmlstream != null)
               xmlstream.close();
        } catch (IOException ex) {
           ex.printStackTrace();
           AALogger.logError(cls, ex.getMessage());  
       }
       if (nonUTF8 > 0)
           AALogger.logInfo(cls,"BPML " + bp_name + " contain " + nonUTF8 + " non UTF8 characters !");
       AALogger.logInfo(cls, bp_name + " created" ); 
     }
     return true;
   }
   /**
    * Create a BP (modeler) file from the workflow  file.
    * @param fileName
    * @param tag
    * @param bp
    * @throws TransformerConfigurationException 
    */
    /*
    public void createBPfile(String filePath, String tag,String bp) throws TransformerConfigurationException{ 
       String fileName = filePath + ".xml";
       String clearText = null;
       int non_UTF8 = 0;
       if (BPExportWrapper.debug)
           System.out.println("XML File :" + fileName + "  tag: " + tag);
       Document doc = loadDocument(fileName);
       doc.getDocumentElement().normalize();
       AALogger.logDebug(cls, "Root element :" + doc.getDocumentElement().getNodeName());
       NodeList nList = doc.getElementsByTagName(tag);
       for (int temp = 0; temp < nList.getLength(); temp++) {
           Node nNode = nList.item(temp);
           if (nNode.getNodeType() == Node.ELEMENT_NODE) {
               Element eElement = (Element) nNode;
               String encode64bpml = eElement.getElementsByTagName("LangResource").item(0).getTextContent();
               clearText = decode(encode64bpml.substring(11));
               encode64bpml = null; // release space
               StringBuilder sb = new StringBuilder();
               sb.append(filePath).append(".bpml");
               try {
                   PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sb.toString())));
                   pw.print(clearText);
                   pw.close();
                   BPFile bpf = BPFile.newFile(filePath + ".bp","UTF8");
                   bpf.writeLangFile(clearText);         
                   clearText=null;
                   sb.setLength(0);
                   // Save gpl in clear text to file
                   sb.append(filePath).append(".gpl");
                   encode64bpml = eElement.getElementsByTagName("GPLResource").item(0).getTextContent();
                   if (encode64bpml != null) {
                       clearText = decode(encode64bpml.substring(11));
                       pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sb.toString())));
                       pw.print(clearText);
                       pw.close();
                       bpf.writeGPLFile(clearText);
                       bpf.close();
                       //clearText=null;
                   }
               } catch (IOException ex) {
                   ex.printStackTrace();
                   AALogger.logError(cls, ex.getMessage());  
               }
              }
       }
    }
    */
    /**
     * doExport
     * @return 
     * loop on a single worklow (bpNameLike or a list of filenames bpFileNameList
     */
    public int doExport() {
       ArrayList bpNames = null;
       boolean r = true;         // for test handling
        if (bpNameLike != null) {
            bpNames = new ArrayList();
            bpNames.add(bpNameLike);
        } else {
            if (bpFileNameList == null){
                AALogger.logError(cls, "No BP or BPList specified for Export -- failed ");    
                return -1;         
            } else {
                    bpNames = readBPlistFile(bpFileNameList);   
            }        
        }
        int len = bpNames.size();
        for (int i = 0; i < len; i++) {
            try {
                String bp = (String)bpNames.get(i);
                if (!dropSI)
                   r = exportBP(bp);
                if (r) {
                    StringBuilder sb = new StringBuilder();
                    //sb.append(dirPath).append(bp);
                    sb.append(dirPath);
                    //sb.append(dirPath).append(bp).append("_exported.xml");
                    AALogger.logInfo(cls, "Updating workflow xml with clear text bpml");  
                    if (!loadDOM(sb.toString(),"BPDEF",bp)) {
                        AALogger.logError(cls, "Export of " + bp + " failed ");
                    }
                }    
            } catch (Exception ex) {
                ex.printStackTrace();
                AALogger.logInfo(cls, ex.getMessage());
                return -1;
            }
        }  
      // processDom();
       return 0;
        
    }
    /**
     * Create a B file (zip file) which can be loaded into GPM (modeler)
     * @return 
     */ 
/*    
    public int doGPM() {
       if (bpNameLike != null) {
            bpNames = new ArrayList();
            bpNames.add(bpNameLike);
        } else {
            readBPlistFile(bpFileNameList);            
        }
        int len = bpNames.size();
        for (int i = 0; i < len; i++) {
            try {
                String bp = (String)bpNames.get(i);
                    StringBuilder sb = new StringBuilder();
                    sb.append(dirPath).append(bp);
                    createBPfile(sb.toString(),"BPDEF",bp);
           } catch (Exception ex) {
               ex.printStackTrace();
               AALogger.logError(cls, ex.getMessage());
           }
        }  
      // processDom();
       return 0;-dirPathvi
        
    }
    
    public int doAddwf() {
       if (bpNameLike != null) {
            bpNames = new ArrayList();
            bpNames.add(bpNameLike);
            
        } else {
            readBPlistFile(bpFileNameList);            
        }
        int len = bpNames.size();
        for (int i = 0; i < len; i++) {
            try {
                String bp = (String)bpNames.get(i);
                StringBuilder sb = new StringBuilder();
                sb.append(dirPath).append(bp);
                addWorkflow(sb.toString(),"BPDEF",bp);
            } catch (Exception ex) {
                 AALogger.logError(cls, ex.getMessage());
            }
        }  
       return 0;
    }
*/    
    public boolean processArgs(String [] args) {
      String validArgs[] ={"-action","-bp","-bplist","-dirpath","-sipath","-dbg","-keep","-dropsi"};
      int argPos;
      if (args.length == 0) {
          AALogger.logInfo(BPExportWrapper.class.getName() +  " No parameters (need properties) ");  
           //showUsage();
          return false;
      }
      int i = 0;
      while(i<args.length)
      {
         argPos = -1;
         for ( int argNum=0; argNum < validArgs.length; argNum++ ) {
            if ( validArgs[argNum].compareTo(args[i]) == 0 ) {
               argPos = argNum;
               break;
            }
         }
         switch ( argPos ) {
            case 0:   // get action token (export,update...
               i++;
               strAction = args[i];
               setAction(strAction);
               break;
            case 1:   // BP name
               i++;
               bpNameLike = args[i];
               break;
            case 2:   // List of BP names
               i++;
               bpFileNameList = args[i];
               break;
            case 3:   // Directory path
               i++;
               dirPath = fixDirName(args[i]);
               break; 
            case 4:   // Directory path
               i++;
               siPath = fixDirName(args[i]);
               break; 
            case 5:
              debug = true;
               break;         
            case 6:
               keepInputXML = true;
               break;
            case 7:
               dropSI = true;
               break;         
            default: // unknown option
               AALogger.logError(cls, " unknown option: " + args[i]);  
               System.out.println(cls + "-    unknown option: " + args[i]);
               return false;
               //showUsage();
               //System.exit(1);
//             break;
         }
         i++;
    }
    return true;
    }
    public boolean doAction(String[] args){
       AALogger.logInfo(cls,"process cl parameters" );
       if (!processArgs(args))
           return false;
        switch (nAction) {
            case 1:     // Export BP from SI
                long start = System.currentTimeMillis();
                AALogger.logInfo(BPExportWrapper.class.getName(),"Action addworkflow"); 
                doExport();
                long end = System.currentTimeMillis();
                AALogger.logInfo(cls, "Export done -- time " + (end-start) + " ms");
                //doAddwf();
                break;
            case 2:
                AALogger.logInfo(BPExportWrapper.class.getName()," Action moved to BPVersionUtil"); 
                //doAddwf();
                break;
            case 3:
                AALogger.logInfo(BPExportWrapper.class.getName() +  " Action moved to BPVersionUtil"); 
                //doGPM();
                break;
            default:
                AALogger.logInfo(BPExportWrapper.class.getName() +  " Action unknown " + nAction); 
                break;
        }
        return true;
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
       BPExportWrapper bpew = new BPExportWrapper();
       if (!bpew.doAction(args))
          System.out.println("BPExportWrapper failed");
     }
}
