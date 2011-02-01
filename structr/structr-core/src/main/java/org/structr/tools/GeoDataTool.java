package org.structr.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.neo4j.index.lucene.LuceneFulltextIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.gis.spatial.ShapefileImporter;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.shell.util.json.JSONObject;
import org.structr.common.RelType;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.geo.GeoObject;

public class GeoDataTool {

    protected static GraphDatabaseService graphDb;
    protected static LuceneFulltextIndexService index;
    public final static String[] northernAmerica = new String[]{
        "Canada",
        "Greenland",
        "Mexico",
        "United States",
        "Greenland"
    };
    public final static String[] centralAmericaAndCaribbean = new String[]{
        "Anguilla",
        "Antigua and Barbuda",
        "Aruba",
        "Antigua and Barbuda",
        "Bermuda",
        "Bahamas",
        "Barbados",
        "Belize",
        "Bermuda",
        "Bonaire",
        "British Virgin Islands",
        "Cayman Islands",
        "Clipperton Island",
        "Costa Rica",
        "Cuba",
        "Curacao",
        "Dominica",
        "Dominican Republic",
        "El Salvador",
        "Grenada",
        "Guadeloupe",
        "Guatemala",
        "Haiti",
        "Honduras",
        "Jamaica",
        "Martinique",
        "Montserrat",
        "Navassa Island",
        "Nicaragua",
        "Panama",
        "Puerto Rico",
        "Saba",
        "Saint Barthelemy",
        "Saint Kitts and Nevis",
        "Saint Lucia",
        "Saint Martin",
        "Saint Pierre and Miquelon",
        "Saint Vincent and the Grenadines",
        "Sint Eustatius",
        "Sint Maarten",
        "Trinidad and Tobago",
        "Turks and Caicos Islands",
        "United States Virgin Islands"
    };
    public final static String[] africa = new String[]{
        "Algeria",
        "Angola",
        "Benin",
        "Botswana",
        "Burkina Faso",
        "Burundi",
        "Cameroon",
        "Cape Verde",
        "Central African Republic",
        "Chad",
        "Comoros",
        "Congo",
        "Cote d'Ivoire",
        "Democratic Republic of the Congo",
        "Djibouti",
        "Egypt",
        "Equatorial Guinea",
        "Eritrea",
        "Ethiopia",
        "Gabon",
        "Gambia",
        "Ghana",
        "Guinea",
        "Guinea-Bissau",
        "Kenya",
        "Lesotho",
        "Liberia",
        "Libyan Arab Jamahiriya",
        "Madagascar",
        "Malawi",
        "Mali",
        "Mauritania",
        "Mauritius",
        "Mayotte",
        "Morocco",
        "Mozambique",
        "Namibia",
        "Niger",
        "Nigeria",
        //"Republic of the Congo",
        "Reunion",
        "Rwanda",
        "Saint Helena",
        "Sao Tome and Principe",
        "Senegal",
        "Seychelles",
        "Sierra Leone",
        "Somalia",
        "South Africa",
        "Sudan",
        "Swaziland",
        //"Tanzania",
        "Togo",
        "Tunisia",
        "Uganda",
        "United Republic of Tanzania",
        "Western Sahara",
        "Zambia",
        "Zimbabwe"
    };
    public final static String[] europe = new String[]{
        "Andorra",
        "Albania",
        "Austria",
        "Belarus",
        "Belgium",
        "Bosnia and Herzegovina",
        "Bulgaria",
        "Croatia",
        "Cyprus",
        "Czech Republic",
        "Denmark",
        "Estonia",
        "Finland",
        "France",
        "Germany",
        "Greece",
        "Hungary",
        "Ireland",
        "Italy",
        "Latvia",
        "Liechtenstein",
        "Lithuania",
        "Luxembourg",
        "The former Yugoslav Republic of Macedonia",
        "Malta",
        "Montenegro",
        "Netherlands",
        "Poland",
        "Portugal",
        "Romania",
        "Serbia",
        "Slovakia",
        "Slovenia",
        "Spain",
        "Sweden",
        "Switzerland",
        "United Kingdom",
        "Ukraine"
    };
    public final static String[] southAmerica = new String[]{
        "Argentina",
        "Bolivia",
        "Brazil",
        "Chile",
        "Colombia",
        "Ecuador",
        "Falkland Islands",
        "French Guiana",
        "Guyana",
        "Paraguay",
        "Peru",
        "Suriname",
        "Uruguay",
        "Venezuela"
    };
    public final static String[] northernAsia = new String[]{
        "Russia",
        "Kyrgyzstan",
        "Armenia",
        "Mongolia",
        "Uzbekistan",
        "Turkmenistan",
        "Pakistan",
        "Tajikistan",
        "Azerbaijan",
        "Georgia",
        "Kazakhsta"
    };
    public final static String[] middleEast = new String[]{
        "Afghanistan",
        "Bahrain",
        "Iran (Islamic Republic of)",
        "Jordan",
        "Israel",
        "United Arab Emirates",
        "Saudi Arabia",
        "Lebanon",
        "Turkey",
        "Egypt",
        "Qatar",
        "Iraq",
        "Kuwait",
        "Oman",
        "Syrian Arab Republic",
        "Palestine",
        "Brunei Darussalam"
    };
    public final static String[] southEastAsia = new String[]{"India", "Bhutan", "China",
        "Lao People's Democratic Republic", "Burma", "Japan",
        "Korea, Democratic People's Republic of", "Korea, Republic of",
        "Philippines", "Nepal", "Indonesia", "Taiwan", "Singapore",
        "Hong Kong", "Thailand", "Viet Nam", "Bangladesh", "Malaysia",
        "Sri Lanka", "Cambodia"
    };
    public final static String[] oceaniaAndAustralia = new String[]{
        "Australia",
        "New Zealand",
        "Papua New Guinea",
        "Antarctica",
        "French Southern and Antarctic Lands",
        "Tokelau",
        "Micronesia, Federated States of",
        "New Caledonia",
        "American Samoa",
        "United States Minor Outlying Islands"
    };

    /**
     * Shapefile importer. Reads a shapefile and creates nodes out of it.
     *
     * Example usage:
     * 
     *      java -cp <classpath> org.structr.tools.ShapefileImporter [-t] -importShapefile|-geocode -dbPath /var/lib/structr -file <filepath>
     *
     * @param args
     */
    public static void main(String[] args) {

        boolean test = false;
        boolean importShapefile = false;
        boolean geocode = false;
        boolean linkCountries = false;

        String dbPath = null;
        String file = null;
        String type = null;
        String layer = null;

        System.out.println("Geodata tool started.");


        System.out.print("Parsing command line arguments ...");

        for (int k = 0; k < args.length; k++) {
            String arg = args[k];

            if ("-t".equals(arg)) {
                test = true;
            } else if ("-importShapefile".equals(arg)) {
                importShapefile = true;
            } else if ("-linkCountries".equals(arg)) {
                linkCountries = true;
            } else if ("-geocode".equals(arg)) {
                geocode = true;
            } else if ("-layer".equals(arg)) {
                layer = args[++k];
            } else if ("-type".equals(arg)) {
                type = args[++k];
            } else if ("-dbPath".equals(arg)) {
                dbPath = args[++k];
            } else if ("-file".equals(arg)) {
                file = args[++k];
            }
        }

        System.out.println("done.");

        if (dbPath == null) {
            System.out.println("Argument -dbPath missing!");
            System.exit(0);
        }

        GeoDataTool adminTool = new GeoDataTool(dbPath, file);

        Transaction tx = graphDb.beginTx();
        try {

            if (importShapefile) {

                System.out.println("Importing shapefile ...");
                adminTool.importShapefile(test, file, layer);

            } else if (geocode) {

                System.out.println("Starting geocoding ...");
                adminTool.geocode(tx, test, type);

            } else if (linkCountries) {

                System.out.println("Linking countries to regions ...");
                adminTool.linkCountries(test);

            }

            tx.success();
        } finally {
            tx.finish();
        }
        shutdown();

        System.out.println("Geodata tool finished.");


    }

    public GeoDataTool(String dbPath, String filesPath) {

        try {
            registerShutdownHook();

            if (graphDb == null) {
                System.out.print("Starting database ...");
                graphDb = new EmbeddedGraphDatabase(dbPath);
                System.out.println("done.");
            }


            if (graphDb != null) {
                index = new LuceneFulltextIndexService(graphDb);
            }

        } catch (Exception e) {
            System.err.print("Could not connect establish database session: "
                    + e.getMessage());
        }
    }

    private static void shutdown() {
        // indexService.shutdown();
        System.out.print("Shutting down database ...");
        graphDb.shutdown();
        System.out.println("done.");
    }

    private static void registerShutdownHook() {
        // Registers a shutdown hook for the Neo4j and index service instances
        // so that it shuts down nicely when the VM exits (even if you
        // "Ctrl-C" the running example before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                shutdown();
            }
        });
    }

    /**
     * Import a shapefile via neo4j-spatial
     *
     * @param file file path on disk
     * @param test if true, don't write to database
     */
    private void importShapefile(boolean test, String file, String layer) {

        try {
            ShapefileImporter importer = new ShapefileImporter(graphDb);
            if (!test) {
                importer.importFile(file, layer);
            }
        } catch (Throwable t) {
            System.out.println("Error while loading data from shapefile " + t.getMessage() + " into layer " + layer);
        }
    }

    /**
     * Geocode objects
     *
     * By looking up Google Maps Geocoding, find the centre coordinates of any
     * node with the given class name and add latitude and longitude to the
     * node's properties.
     *
     * @param className full qualified name of object class
     * @param test if true, output geocoding results
     */
    private void geocode(Transaction tx, boolean test, String type) {

        for (Node n : graphDb.getAllNodes()) {

            if (n.hasProperty(StructrNode.TYPE_KEY) && n.getProperty(StructrNode.TYPE_KEY).equals(type)
                    && n.hasProperty(GeoObject.LATITUDE_KEY)
                    && (
                           ((Double) n.getProperty(GeoObject.LATITUDE_KEY)).equals(Double.NaN)
                        || ((Double) n.getProperty(GeoObject.LATITUDE_KEY)).equals(0.0))
                    ) {

                StringBuilder address = new StringBuilder();

                // if available, try to add the properties
                // name, street, zip, city and country
                // to the address string
                String[] addressKeys = new String[]{"name", "street", "zip", "city", "country"};
                for (String key : addressKeys) {

                    if (n.hasProperty(key)) {
                        if (address.length() > 0) {
                            address.append(", ");
                        }
                        address.append((String) n.getProperty(key));
                    }
                }

                // use set to avoid duplicate entries
                Set<String> additionalAddressAttributes = new HashSet<String>();

                // if the geo object is contained in another node, add those names to the address
                for (Relationship r : n.getRelationships(Direction.INCOMING)) {

                    Node s = r.getStartNode();

                    if (s.hasProperty(StructrNode.TYPE_KEY) && s.hasProperty(StructrNode.NAME_KEY)) {

                        String addType = (String) s.getProperty(StructrNode.TYPE_KEY);

                        if (!("Country".equals(addType))
                                && !("Island".equals(addType))
                                && !("State".equals(addType))) {
                            continue;
                        }


                        additionalAddressAttributes.add((String) s.getProperty(StructrNode.NAME_KEY));
                    }
                }

                for (String s : additionalAddressAttributes) {
                    if (address.length() > 0) {
                        address.append(", ");
                    }
                    address.append(s);
                }


                JSONObject json = null;
                URL mapsUrl = null;

                try {

                    String encodedAddress = URLEncoder.encode(address.toString(), "UTF-8");

                    String protocol = "xml"; // "xml" or "json"

                    mapsUrl = new URL("http://maps.google.com/maps/api/geocode/" + protocol + "?sensor=false&address=" + encodedAddress);
                    HttpURLConnection connection = (HttpURLConnection) mapsUrl.openConnection();
                    connection.connect();

                    if (protocol.equals("json")) {

                        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                        StringBuilder result = new StringBuilder();
                        String line;
                        while ((line = rd.readLine()) != null) {
                            result.append(line);
                        }
                        connection.disconnect();

                        if (result != null) {
                            System.out.println("Result: " + result.toString());
                            try {
                                json = new JSONObject(result.toString());
                                System.out.println("Latitude: " + json.getString("geometry.location.lat"));
                            } catch (Exception e) {
                                System.out.println("Couldn't create JSON object from result string: " + result.toString());
                            }

                        }

                    } else if (protocol.equals("xml")) {

                        Document xmlDoc = null;
                        SAXReader reader = new SAXReader();

                        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        xmlDoc = reader.read(rd);
                        connection.disconnect();
                        rd.close();

                        Element root = xmlDoc.getRootElement();
                        //List<Element> rootChildren = root.elements();


                        String status = root.element("status").getTextTrim();
                        boolean ok = "OK".equals(status);

                        if (!ok) {
                            System.out.println("Status not OK for " + type + " with address " + address + ": " + status);
                        } else {

                            String latitude = root.element("result").element("geometry").element("location").element("lat").getTextTrim();
                            String longitude = root.element("result").element("geometry").element("location").element("lng").getTextTrim();

                            double lat = Double.parseDouble(latitude);
                            double lon = Double.parseDouble(longitude);

                            System.out.println("Coordinates found for " + type + " with address " + address + ": lat= " + lat + ", lon=" + lon);

                            String latKey = GeoObject.LATITUDE_KEY;
                            String lonKey = GeoObject.LONGITUDE_KEY;

                            if (!test) {
                                n.setProperty(latKey, lat);
                                index.removeIndex(latKey);
                                index.index(n, latKey, lat);

                                n.setProperty(lonKey, lon);
                                index.removeIndex(lonKey);
                                index.index(n, lonKey, lon);

                                System.out.println("Node updated with new coordinates");
                            }
                        }

                    }
                    // commit each transactions to the database
                    tx.success();
                    tx.finish();
                    tx = graphDb.beginTx();

                    Thread.sleep(200);

                } catch (Exception e) {
                    System.out.println("Error while fetching content from URL " + mapsUrl + ": " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    //if (stream != null) stream.close();
                }


                //System.out.println(json.toString());
            }


        }
    }

    private void linkCountries(final boolean test) {

        Map<String, String[]> regionMap = new HashMap<String, String[]>();


        regionMap.put("Northern America", northernAmerica);
        regionMap.put("Central America and Caribbean", centralAmericaAndCaribbean);
        regionMap.put("South America", southAmerica);
        regionMap.put("Africa", africa);
        regionMap.put("Europe", europe);
        regionMap.put("Northern Asia", northernAsia);
        regionMap.put("Middle East", middleEast);
        regionMap.put("South East Asia", southEastAsia);
        regionMap.put("Oceania and Australia", oceaniaAndAustralia);

        Map<String, Node> nodeMap = new HashMap<String, Node>();

        Iterable<Node> allNodes = graphDb.getAllNodes();

        Node countriesFolder = null;

        // find countries node
        for (Node n : allNodes) {

            if (n.hasProperty(StructrNode.TYPE_KEY) && n.getProperty(StructrNode.TYPE_KEY).equals("Folder")
                    && n.hasProperty(StructrNode.NAME_KEY) && n.getProperty(StructrNode.NAME_KEY).equals("Countries")) {
                countriesFolder = n;
            }
        }


        int regionCounter = 0;
        // find nodes of the global regions
        for (Node n : allNodes) {
            if (n.hasProperty(StructrNode.TYPE_KEY) && n.getProperty(StructrNode.TYPE_KEY).equals("Region")) {

                for (String region : regionMap.keySet()) {

                    if (n.hasProperty(StructrNode.NAME_KEY) && n.getProperty(StructrNode.NAME_KEY).equals(region)) {
                        nodeMap.put(region, n);
                        break;
                    }
                    regionCounter++;
                }
            }
        }

        System.out.println("Processed " + regionCounter + " regions");

        for (String region : regionMap.keySet()) {

            Node regionNode = (Node) nodeMap.get(region);

            if (regionNode != null) {

                // remove existing relationships between region and countries
                Iterable<Relationship> rels = regionNode.getRelationships(RelType.LINK, Direction.OUTGOING);
                if (rels != null) {
                    for (Relationship r : rels) {
                        Node endNode = r.getEndNode();
                        if (endNode.hasProperty(StructrNode.TYPE_KEY) && endNode.getProperty(StructrNode.TYPE_KEY).equals("Country")) {
                            r.delete();
                        }
                    }
                }

                String[] countries = (String[]) regionMap.get(region);
                for (String country : countries) {

                    System.out.println("Processing country " + country);


                    // find country node
                    for (Node n : allNodes) {

                        // TODO: handle slightly different names as equal
                        if (n.hasProperty(StructrNode.TYPE_KEY) && n.getProperty(StructrNode.TYPE_KEY).equals("Country")
                                && n.hasProperty(StructrNode.NAME_KEY) && n.getProperty(StructrNode.NAME_KEY).equals(country)) {

                            if (!test) {
                                regionNode.createRelationshipTo(n, RelType.LINK);
                                System.out.println("Linked " + country + " to " + region + " node");
                                break;
                            }

                        } else {
                            //System.out.println("Country " + country + " doesn't exist");
//                            if (!test) {
//
//                                Node newCountryNode = graphDb.createNode();
//
//                                newCountryNode.setProperty(StructrNode.TYPE_KEY, "Country");
//                                newCountryNode.setProperty(StructrNode.NAME_KEY, country);
//
//                                if (countriesFolder != null) {
//                                    countriesFolder.createRelationshipTo(newCountryNode, RelType.HAS_CHILD);
//                                }
//
//                                regionNode.createRelationshipTo(newCountryNode, RelType.LINK);
//                                System.out.println("Linked " + country + " to " + region + " node");
//
//                                break;
//
//                            }
                        }
                    }
                }
            }
        }



    }
}
