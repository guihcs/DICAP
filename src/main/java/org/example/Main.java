package org.example;

import eu.sealsproject.platform.res.tool.api.ToolBridgeException;
import org.apache.commons.lang3.time.StopWatch;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import utils.Pair;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Main {


    private static final OWLDataFactory factory = new OWLDataFactoryImpl();
    static CallCanard canard;
    static CallLinkex linkex;
    static CallLogMap logMap;
    static CallAMLC amlc;

    private static File fileSource;
    private static File fileTarget;


    public static void main(String[] args) throws Exception {
       /* String pathSource = "edas_100.ttl";

        String pathTarget = "conference_100.ttl";

        Double valueOfConf = 0.7;
        String system="2";*/

        //  Scanner scanner = new Scanner(System.in);
        //  System.out.println("Which system you need to integrate? 1:Atmatcher, 2:Canard, 3:LogMap, 4:AMD, 5:Matcha");
        String system = args[0];
        //  System.out.println("Please enter the source ontology");
        String pathSource = args[1];
        //scanner.nextLine().trim();
        //System.out.println("Please enter the target ontology");
        String pathTarget = args[2];
        //scanner.nextLine().trim();


        fileSource = new File("test/" + pathSource);
        fileTarget = new File("test/" + pathTarget);


        if (system.equals("1")) {
            runAtMatcher(fileSource, fileTarget, "/usr/bin/java", "/Users/khadijajradeh/Downloads/DICAPNEW/matchers/atm/atmatcher-1.0.jar", "/Users/khadijajradeh/Downloads/DICAPNEW/output/");

        } else if (system.equals("2")) {
            System.out.println("Please enter the value of confidence");
            Double valueOfConf = Double.valueOf(args[4]);
            pipeCanard(fileSource, fileTarget, fileSource, fileTarget, valueOfConf, "/Users/khadijajradeh/Downloads/DICAPNEW/output/");
            //

            pipeLogmap(fileSource, fileTarget, "/usr/bin/java", "/Users/khadijajradeh/Downloads/logmap-matcher-4.0.jar", "/Users/khadijajradeh/Downloads/DICAPNEW/output/");
            //
        } else if (system.equals("4")) {
            runAMD(fileSource, fileTarget, "/usr/bin/java", "output", "/Users/khadijajradeh/Downloads/DICAPNEW/output/");
        } else if (system.equals("5")) {
            runMatcha(fileSource, fileTarget, "/usr/bin/java", "/Users/khadijajradeh/Downloads/matcha/external/oaei-0.0.1-SNAPSHOT.jar", "/Users/khadijajradeh/Downloads/DICAPNEW/output/");
        }



    }


    private static void runMatcha(File fileSource, File fileTarget, String java, String matchaPath, String output) throws IOException, ToolBridgeException, OWLOntologyStorageException, ParserConfigurationException, SAXException {
        OWLOntology source = loadOntology(fileSource);
        OWLOntology target = loadOntology(fileTarget);
        for (int iter = 0; iter < 5; iter++) {

            String fs = CallMatcha.run(fileSource.getAbsolutePath(), fileTarget.getAbsolutePath(), matchaPath);

            Correspondence.saturateCorrespondenceSimple(source, target, fs);
            Linkey.saturateSameAs(source, target, fs);

            fileSource = new File("test/source_tmp.ttl");
            fileTarget = new File("test/target_tmp.ttl");
            OWLOntologyManager manager1 = source.getOWLOntologyManager();
            OWLOntologyManager manager2 = target.getOWLOntologyManager();
            manager1.saveOntology(source, source.getFormat(), IRI.create(fileSource.toURI()));
            manager2.saveOntology(target, target.getFormat(), IRI.create(fileTarget.toURI()));
        }
    }

    private static void runAtMatcher(File fileSource, File fileTarget, String java, String path, String output) throws Exception {
        System.out.println("piping Atmatcher started");
        CallAtMatcher callAtMatcher = new CallAtMatcher(java, path);
       // System.out.println("piping Atmatcher started 0");
        Runtime.getRuntime().addShutdownHook(new Thread(callAtMatcher::close));
        //System.out.println("piping Atmatcher started 1");

      //  System.out.println("piping Atmatcher started 1");
        OWLOntology source = loadOntology(fileSource);
        OWLOntology target = loadOntology(fileTarget);
        //System.out.println("piping Atmatcher started 2");
        for (int iter = 1; iter < 3; iter++) {

            System.out.println("Iteration: " + iter);

            String fs = output + "atmr"+ iter+".rdf";
            callAtMatcher.start();
            callAtMatcher.run(fileSource.getAbsolutePath(), fileTarget.getAbsolutePath(), fs);
            callAtMatcher.close();
            System.out.println("Atmatcher called");
            //  Set<Alignment> s=Alignment.readAlignmentsAt(fs);
            Correspondence.saturateCorrespondenceSimple(source, target, fs);
            System.out.println("Ontologies saturated");
            Linkey.saturateSameAs(source, target, fs);

            fileSource = new File(output + "source_tmp.xml");
            fileTarget = new File(output + "target_tmp.xml");
            OWLOntologyManager manager1 = source.getOWLOntologyManager();
            OWLOntologyManager manager2 = target.getOWLOntologyManager();
            manager1.saveOntology(source, new RDFXMLDocumentFormat(), IRI.create(fileSource.toURI()));
            manager2.saveOntology(target, new RDFXMLDocumentFormat(), IRI.create(fileTarget.toURI()));
            removeAnnotationProperty("output/source_tmp.xml", "output/source_tmp.xml");
            removeAnnotationProperty("output/target_tmp.xml", "output/target_tmp.xml");

        }


    }


    private static void pipeLogmap(File fileSource, File fileTarget, String java, String logmap, String output) throws OWLOntologyStorageException, IOException, ParserConfigurationException, SAXException {


        StopWatch pipe = new StopWatch();
        pipe.start();
        OWLOntology source = loadOntology(fileSource);
        OWLOntology target = loadOntology(fileTarget);

        logMap = new CallLogMap("/Users/khadijajradeh/Downloads/logmap-matcher-standalone-july-2021/logmap-matcher-4.0.jar");
        pipe.stop();
        runLM(fileSource, fileTarget, source, target);
    }

    private static void runLM(File fileSource, File fileTarget, OWLOntology source, OWLOntology target) throws OWLOntologyStorageException, IOException, ParserConfigurationException, SAXException {

        String output = "output";
        for (int iter = 0; iter < 5; iter++) {


            logMap.execute(fileSource.getAbsolutePath(), fileTarget.getAbsolutePath(), output);
            String fs = output + "/logmap_overestimation.txt";
            Correspondence.saturateCorrespondenceSimple(source, target, fs);
            Linkey.saturateSameAs(source, target, fs);


            fileSource = new File("test/source_tmp.owl");
            fileTarget = new File("test/target_tmp.owl");
            OWLOntologyManager manager1 = source.getOWLOntologyManager();
            OWLOntologyManager manager2 = target.getOWLOntologyManager();
            manager1.saveOntology(source, new RDFXMLDocumentFormat(), IRI.create(fileSource.toURI()));
            manager2.saveOntology(target, new RDFXMLDocumentFormat(), IRI.create(fileTarget.toURI()));

        }
    }


    private static void runAMD(File fileSource, File fileTarget, String python, String amdPath, String output) throws OWLOntologyStorageException, IOException, InterruptedException, ParserConfigurationException, SAXException {
        OWLOntology source = loadOntology(fileSource);
        OWLOntology target = loadOntology(fileTarget);
        CallAMD amd = new CallAMD(python, amdPath);
        for (int iter = 0; iter < 5; iter++) {

            amd.run(fileSource.getAbsolutePath(), fileTarget.getAbsolutePath(), output);
            String fs = output + "out.txt";
            Correspondence.saturateCorrespondenceSimple(source, target, fs);
            Linkey.saturateSameAs(source, target, fs);

            fileSource = new File(output + "source_tmp.owl");
            fileTarget = new File(output + "target_tmp.owl");
            OWLOntologyManager manager = source.getOWLOntologyManager();
            OWLOntologyManager manager2 = source.getOWLOntologyManager();
            manager.saveOntology(source, new RDFXMLDocumentFormat(), IRI.create(fileSource.toURI()));
            manager2.saveOntology(target, new RDFXMLDocumentFormat(), IRI.create(fileTarget.toURI()));
        }
    }


    static OWLOntology loadOntology(File fileSource) {

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = null;
        try {
            ontology = manager.loadOntologyFromOntologyDocument(fileSource);

        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        return ontology;
    }

    static void pipeCanard(File fileSourceI, File fileTargetI, File fileSource, File fileTarget, Double valueOfConf, String output) throws Exception {
        StopWatch pipe = new StopWatch();

        pipe.start();

        OWLOntology source = loadOntology(fileSourceI);
        OWLOntology target = loadOntology(fileTargetI);


        linkex = new CallLinkex("../linkex/LinkkeyDiscovery-1.0-SNAPSHOT-jar-with-dependencies.jar");
        File f_start = new File("output/startlinkeys");
        f_start.createNewFile();
        Set<Linkey> lks = linkex.execute(fileSourceI, fileTargetI, f_start);


        canard = new CallCanard("../canard/CanardE.jar");
        canard.execute(fileSourceI, fileTargetI, fileSource, fileTarget, valueOfConf);

        int t1 = fileSource.getName().lastIndexOf(".");
        int t2 = fileTarget.getName().lastIndexOf(".");

        String fs = "output/" + fileSourceI.getName().substring(0, t1) + "_" + fileTargetI.getName().substring(0, t2) + "/th_" + valueOfConf.toString() + ".edoal";
        ParseEdoal pr = new ParseEdoal();
        Correspondence c = new Correspondence();
        runCanard(fileSourceI, fileTargetI, valueOfConf, source, target, lks);
        pipe.stop();
        System.out.println("Pipe: " + pipe.getTime(TimeUnit.SECONDS));
    }


    static void pipeAMLC(File fileSourceI, File fileTargetI, File fileSource, File fileTarget, Double valueOfConf) throws Exception {
        StopWatch pipe = new StopWatch();

        pipe.start();
        OWLOntology source = loadOntology(fileSourceI);
        OWLOntology target = loadOntology(fileTargetI);
        linkex = new CallLinkex("../linkex/LinkkeyDiscovery-1.0-SNAPSHOT-jar-with-dependencies.jar");

        File f_start = new File("output/startlinkeys");

        if (!f_start.exists()) {
            f_start.createNewFile();
        }

        Set<Linkey> lks = linkex.execute(fileSourceI, fileTargetI, f_start);

        System.out.println(valueOfConf);
        amlc = new CallAMLC("../AML/bin/AML.jar");
        amlc.execute(fileSource, fileTarget);
        //  canard = new CallCanard( "../canard/CanardE.jar");
        // canard.execute(fileSourceI, fileTargetI, fileSource, fileTarget, valueOfConf);

        int t1 = fileSource.getName().lastIndexOf(".");
        int t2 = fileTarget.getName().lastIndexOf(".");

        String fs = "output/" + fileSource.getName().substring(0, t1) + "_" + fileTarget.getName().substring(0, t2) + "/th_" + valueOfConf.toString() + ".edoal";


        // removeRdfsLabels(source);
        //  removeRdfsLabels(target);

        ParseEdoal pr = new ParseEdoal();
        // pr.saveOntologies(source, fileSourceI);
        //  pr.saveOntologies(target, fileTargetI);

        Correspondence c = new Correspondence();
        //   Correspondence.separateCorrespondences(fs);


        // canard.execute(fileSourceI, fileTargetI, fileSource, fileTarget, valueOfConf);
        amlc.execute(fileSource, fileTarget);

        pipe.stop();
        System.out.println("Pipe: " + pipe.getTime(TimeUnit.SECONDS));
        runAMLC(fileSourceI, fileTargetI, valueOfConf, source, target, lks, fs, pr, c);

    }


    private static void runAMLC(File fileSourceI, File fileTargetI, Double valueOfConf, OWLOntology source, OWLOntology target, Set<Linkey> lks, String fs, ParseEdoal pr, Correspondence c) throws IOException, ParserConfigurationException, SAXException, OWLOntologyStorageException {


        int counter_lks = 0;
        int counter_lkc = 0;

        for (int iter = 0; iter < 2; iter++) {
            System.out.println("Iteration number: " + iter++);
            // int t1 = fileSource.getName().lastIndexOf(".");
            //  int t2 = fileTarget.getName().lastIndexOf(".");
            //  fs = "output/" + fileSource.getName().substring(0, t1) + "_" + fileTarget.getName().substring(0, t2) + "/th_" + valueOfConf + ".edoal";
            //saturate with the starting lks in the first round.

            fileSource = new File("test/source_temp.ttl");
            fileTarget = new File("test/target_temp.ttl");
            pr.saveOntologies(source, fileSource);
            pr.saveOntologies(target, fileTarget);


            canard.execute(fileSourceI, fileTargetI, fileSource, fileTarget, valueOfConf);
            Pair<Set<Correspondence>, Set<Correspondence>> pair = buildCorrespondences(fs);

            for (Correspondence cr : pair.first()) {
                System.out.println();
                File f = new File("output/linkeys_simple/" + cr.getC1().toString().substring(1, cr.getC1().toString().length() - 1).replace("://", "_") + "_" + cr.getC2().toString().substring(1, cr.getC2().toString().length() - 1).replace("://", "_"));

                Set<Linkey> lks_s = linkex.execute(fileSource, fileTarget, f, cr);

                for (Linkey lk : lks_s) {


                    lk.printLk();
                    lk.saturateLinkey(source, target);
                }


                Set<Linkey> lks_w = linkex.execute(fileSource, fileTarget, new File("output/linkeys"));

                if (lks.size() == lks_w.size()) {
                    break;
                }
                lks = lks_w;
            }

        }
    }


    private static void runCanard(File fileSourceI, File fileTargetI, Double valueOfConf, OWLOntology source, OWLOntology target, Set<Linkey> lks) throws IOException, ParserConfigurationException, SAXException, OWLOntologyStorageException {


        for (int iter = 0; iter < 2; iter++) {


            // removeRdfsLabels(source);
            // removeRdfsLabels(target);
            System.out.println("Iteration: " + iter++);

            File fileSource = new File("test/source_temp.ttl");
            File fileTarget = new File("test/target_temp.ttl");
            int t1 = fileSource.getName().lastIndexOf(".");
            int t2 = fileTarget.getName().lastIndexOf(".");
            String fs = "output/" + fileSource.getName().substring(0, t1) + "_" + fileTarget.getName().substring(0, t2) + "/th_" + valueOfConf + ".edoal";
            saturateOntologies(source, target, lks, fs);
            System.out.println("Ontologies saturated!");
            OWLOntologyManager manager1 = source.getOWLOntologyManager();
            OWLOntologyManager manager2 = target.getOWLOntologyManager();
            manager1.saveOntology(source, source.getFormat(), IRI.create(fileSource.toURI()));
            manager2.saveOntology(target, target.getFormat(), IRI.create(fileTarget.toURI()));

            System.out.println("Ontologies saved!");

            canard.execute(fileSourceI, fileTargetI, fileSource, fileTarget, valueOfConf);
            System.out.println("Canard Called!");
            Pair<Set<Correspondence>, Set<Correspondence>> pair = buildCorrespondences(fs);
            Set<Linkey> lks_w = linkex.execute(fileSource, fileTarget, new File("output/linkeys"));
            System.out.println("Linkex Called!");


     /*  for (Correspondence crc : pair.second())
        {
            Set<Linkey> lks_c= linkex.execute(fileSource, fileTarget, new File("output/linkeys_complex/" + crc.getC1().toString().substring(1,crc.getC1().toString().length()-1).replace("://","_")  + "_" + crc.getC2().toString().substring(1,crc.getC2().toString().length()-1).replace("://","_")), crc);

            for (Linkey lk : lks_c) {

                lk.printLk();
                lk.saturateLinkey(source, target);
            }
        }*/

            //  lks_w = linkex.execute(fileSource, fileTarget, new File("output/linkeys"));
        /*if (lks.size() == lks_w.size()) {
            break;
        }*/
            //
            lks = lks_w;
            //
            //  saturateOntologies(source, target, lks, fs);
        }

    }

    private static void saturateOntologies(OWLOntology source, OWLOntology target, Set<Linkey> lks, String fs) throws IOException, ParserConfigurationException, SAXException, OWLOntologyStorageException {

        //  Correspondence c=new Correspondence();
        // c.saturateCorrespondence(source, target, fs);

        //  if (lks.isEmpty()) return;

        for (Linkey lk : lks) {
            //   lk.printLk();
            lk.saturateLinkey(source, target);
        }


    }


    private static Pair<Set<Correspondence>, Set<Correspondence>> buildCorrespondences(String fs) throws IOException, ParserConfigurationException, SAXException {
        Set<Alignment> alignments = Alignment.readAlignments(fs);


        Set<Correspondence> crs = new HashSet<>();
        Set<Correspondence> crsc = new HashSet<>();

        for (Alignment a : alignments) {

            String a1Merged = a.getElement1().toMergedForm();
            String a1Trim = a1Merged.trim();

            String a2Merged = a.getElement2().toMergedForm();
            String a2Trim = a2Merged.trim();

            addTocrs(crs, a1Merged, a1Trim, a2Merged, a2Trim);
            addTocrsc(crsc, a1Merged, a1Trim, a2Merged, a2Trim);
        }

        return new Pair<>(crs, crsc);
    }

    private static void addTocrsc(Set<Correspondence> crsc, String a1Merged, String a1Trim, String a2Merged, String a2Trim) {
        if (!a1Trim.startsWith("Relation") && !a2Trim.startsWith("Relation")
                && a1Trim.startsWith("Class") && a2Trim.startsWith("AttributeDomainRestriction")
                && !a2Trim.contains("or+") && !a2Trim.contains("inverse")
                && !a2Trim.contains("and+")) {

            crsc.add(new Correspondence(factory.getOWLClass(IRI.create(a1Merged)), factory.getOWLClass(IRI.create(a2Merged))));

        }
    }

    private static void addTocrs(Set<Correspondence> crs, String a1Merged, String a1Trim, String a2Merged, String a2Trim) {
        if (!a1Trim.startsWith("Relation") && !a2Trim.startsWith("Relation")
                && a1Trim.startsWith("Class") && a2Trim.startsWith("Class")
                && !a2Trim.contains("or+") && !a2Trim.contains("and+")) {

            crs.add(new Correspondence(factory.getOWLClass(IRI.create(a1Merged)), factory.getOWLClass(IRI.create(a2Merged))));
        }
    }

    private static int getCounterLks(File fileTarget, File fileSource, Set<Correspondence> crs) throws IOException, ParserConfigurationException, SAXException {
        int counter_lks = 0;
        for (Correspondence cc : crs) {
            File f = new File("output/" + cc);
            Set<Linkey> lqeq = linkex.execute(fileSource, fileTarget, f, cc);
            for (Linkey lk : lqeq) {
                lk.printLk();
                counter_lks++;
            }
        }
        return counter_lks;
    }


    public static double calculateAverageMeasure(String xmlFilePath) {
        try {
            File inputFile = new File(xmlFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("measure");
            double sum = 0;
            for (int i = 0; i < nList.getLength(); i++) {
                Element element = (Element) nList.item(i);
                sum += Double.parseDouble(element.getTextContent());
            }
            return sum / nList.getLength();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void removeRdfsLabels(OWLOntology ontology) {
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLAnnotationProperty rdfsLabel = factory.getRDFSLabel();

        List<RemoveAxiom> removeAxiomList = ontology.getAxioms()
                .stream()
                .filter(axiom -> axiom.getAnnotations().stream()
                        .anyMatch(annotation -> annotation.getProperty().equals(rdfsLabel)))
                .map(axiom -> new RemoveAxiom(ontology, axiom))
                .collect(Collectors.toList());

        manager.applyChanges(removeAxiomList);

    }

    public static void removeAnnotationProperty(String filePath, String outputFilePath) throws ParserConfigurationException, IOException, SAXException, TransformerException {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(filePath));

            NodeList nodeList = document.getElementsByTagName("owl:AnnotationProperty");
            for (int i = nodeList.getLength() - 1; i >= 0; i--) {
                Element element = (Element) nodeList.item(i);
                Node parent = element.getParentNode();
                parent.removeChild(element);
                }


            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            Result output = new StreamResult(new File(outputFilePath));
            Source input = new DOMSource(document);
            transformer.transform(input, output);
        }


}
