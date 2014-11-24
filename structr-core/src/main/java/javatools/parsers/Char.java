package javatools.parsers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

import javatools.datatypes.FinalMap;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).
  

  

  This class provides static methods to <I>decode, encode</I> and <I>normalize</I> Strings.<BR>
  <B>Decoding</B> converts the following codes to Java 16-bit characters (<TT>char</TT>):
  <UL>
   <LI>all HTML ampersand codes (like &amp;nbsp;) as specified by the W3C
   <LI>all backslash codes (like \ b) as specified by the Java language specification
   <LI>all percentage codes (like %2C) as used in URLs and E-Mails
   <LI>all UTF-8 codes (like ī) as specified in Wikipedia
  </UL>
  <P>
  <B>Encoding</B> is the inverse operation. It takes a Java 16-bit character (<TT>char</TT>) and
  outputs its encoding in HTML, as a backslash code, as a percentage code or in UTF8.
  <P>
  <B>Normalization</B> converts the following Unicode characters (Java 16-bit <TT>char</TT>s)
  to ASCII-characters in the range 0x20-0x7F:
  <UL>
   <LI>all ASCII control characters (0x00-0x1F)
   <LI>all Latin-1 characters (0x80-0xFF) to the closest transliteration
   <LI>all Latin Extended-A characters (0x100-0x17F) to the closest transliteration
   <LI>all Greek characters (0x374-0x3D6) to the closest transliteration as specified in Wikipedia
   <LI>all General-Punctuation characters (0x2016-0x2055) to the closest ASCII punctuation
   <LI>most mathematical symbols (in the range of 0x2000) to the common program code identifier or text
   <LI>all ligatures (0xFB00-0xFB06, the nasty things you get when you copy/paste from PDFs) to
       the separate characters
  </UL>
  <P>
  <CENTER><H3>Usage</H3></CENTER>
  <P>
  <B>Decoding</B> is done by methods that "eat" a code from the string.
  They require as an additional parameter an integer array of length 1,
  in which they store the length of the code that they chopped off.<BR>
  Example:
  <PRE>
     int[] eatLength=new int[1];
     char c=eatPercentage("%2Cblah blah",eatLength);
     -->  c=','
          eatLength[0]=3  // the code was 3 characters long
  </PRE>
  There is a static integer array Char.eatLength, which you can use for this purpose.
  The methods store 0 in case the String does not start with the correct code.
  They store -1 in case the String starts with a corrupted code. Of course, you can
  use the <TT>eat...</TT> methods also to decode one single code. There are methods
  <TT>decode...</TT> that decode the percentage code, the UTF8-codes, the backslash codes
  or the Ampersand codes, respectively. 
  The method <TT>decode(String)</TT> decodes all codes of a String.<BR>
  Example:
  <PRE>
     decode("This String contains some codes: &amp;amp; %2C \ u0041");
     --> "This String contains some codes: &amp; , A"
  </PRE>
  <P>
  <B>Normalization</B> is done by the method <TT>normalize(int c)</TT>. It converts a Unicode 
  character (a 16-bit Java character <TT>char</TT>)
  to a sequence of normal characters (i.e. characters in the range 0x20-0x7F). 
  The transliteration may consist of multiple chars (e.g. for umlauts) and also of no
  chars at all (e.g. for Unicode Zero-Space-Characters). <BR>
  Example:
  <PRE>
    normalize('&auml;');
    --> "ae"
  </PRE>  
  The method <TT>normalize(String)</TT>  normalizes all characters in a String.<BR>
  Example:
  <PRE>
     normalize("This String contains the umlauts �, � and �");
     -->  "This String contains the umlauts Ae, Oe and Ue"
  </PRE>
  If the method cannot find a normalization, it calls defaultNormalizer.apply(char c).
  Decoding and normalizing can be combined by the method decodeAndNormalize(String s).
  <P>
  <B>Encoding</B> is done by methods called <TT>encode...(char)</TT>. These methods take a character
  and transform it to a UTF8 code, a percentage code, an ampersand code or a backslash code,
  respectively. If the character is normal (i.e. in the range 0x20-0x7F), they simply return the input
  character without any change.<BR>
  Example:
  <PRE>
     encodePercentage('�');
     -->  "%C4"
  </PRE>  
  There are also methods that work on entire Strings<BR>
  Example:
  <PRE>
     encodePercentage("This String contains the umlauts �, � and �");
     -->  "This String contains the umlauts %C4, %D6 and %DC;"
  </PRE>  
  <P>
  Last, this class provides the character categorization for URIs, as given in
  http://tools.ietf.org/html/rfc3986 . It also provides a method to encode only those
  characters that are not valid path component characters<BR>
  Example:
  <PRE>
     isReserved(';');
     -->  true
     encodeURIPathComponent("a: b")
     -->  "a:%20b"
  </PRE>    
 */
public class Char {

  /** Defines just one function from an int to a String */
  public interface Char2StringFn {

    /** Function from a char to a String */
    String apply(char c);
  }

  /** Called by normalize(int) in case the character cannot be normalized.
   * The default implementation returns UNKNOWN.
   * Feel free to create a new Char2StringFn and assign it to defaultNormalizer. */
  public static Char2StringFn defaultNormalizer = new Char2StringFn() {

    public String apply(char c) {
      return (UNKNOWN);
    }
  };

  /** String returned by the default implementation of defaultNormalizer, "[?]"*/
  public static String UNKNOWN = "[?]";

  /** Maps a special character to a HTML ampersand sequence */
  public static Map<Character, String> charToAmpersand = new FinalMap<Character, String>('&', "&amp;", '\'', "&apos;", '<', "&lt;", '>', "&gt;", '"', "&quot;");

  /** Maps a special character to a backslash sequence */
  public static Map<Character, String> charToBackslash= new FinalMap<Character, String>('\\', "\\\\", '\n', "\\n");

  /** Maps HTML ampersand sequences to strings */
  public static Map<String, Character> ampersandMap = new FinalMap<String, Character>("nbsp", (char) 160, "iexcl", (char) 161, "cent", (char) 162, "pound", (char) 163, "curren", (char) 164, "yen", (char) 165, "brvbar", (char) 166, "sect",
      (char) 167, "uml", (char) 168, "copy", (char) 169, "ordf", (char) 170, "laquo", (char) 171, "not", (char) 172, "shy", (char) 173, "reg", (char) 174, "macr", (char) 175, "deg", (char) 176, "plusmn", (char) 177, "sup2", (char) 178, "sup3",
      (char) 179, "acute", (char) 180, "micro", (char) 181, "para", (char) 182, "middot", (char) 183, "cedil", (char) 184, "sup1", (char) 185, "ordm", (char) 186, "raquo", (char) 187, "frac14", (char) 188, "frac12", (char) 189, "frac34", (char) 190,
      "iquest", (char) 191, "Agrave", (char) 192, "Aacute", (char) 193, "Acirc", (char) 194, "Atilde", (char) 195, "Auml", (char) 196, "Aring", (char) 197, "AElig", (char) 198, "Ccedil", (char) 199, "Egrave", (char) 200, "Eacute", (char) 201,
      "Ecirc", (char) 202, "Euml", (char) 203, "Igrave", (char) 204, "Iacute", (char) 205, "Icirc", (char) 206, "Iuml", (char) 207, "ETH", (char) 208, "Ntilde", (char) 209, "Ograve", (char) 210, "Oacute", (char) 211, "Ocirc", (char) 212, "Otilde",
      (char) 213, "Ouml", (char) 214, "times", (char) 215, "Oslash", (char) 216, "Ugrave", (char) 217, "Uacute", (char) 218, "Ucirc", (char) 219, "Uuml", (char) 220, "Yacute", (char) 221, "THORN", (char) 222, "szlig", (char) 223, "agrave",
      (char) 224, "aacute", (char) 225, "acirc", (char) 226, "atilde", (char) 227, "auml", (char) 228, "aring", (char) 229, "aelig", (char) 230, "ccedil", (char) 231, "egrave", (char) 232, "eacute", (char) 233, "ecirc", (char) 234, "euml",
      (char) 235, "igrave", (char) 236, "iacute", (char) 237, "icirc", (char) 238, "iuml", (char) 239, "eth", (char) 240, "ntilde", (char) 241, "ograve", (char) 242, "oacute", (char) 243, "ocirc", (char) 244, "otilde", (char) 245, "ouml",
      (char) 246, "divide", (char) 247, "oslash", (char) 248, "ugrave", (char) 249, "uacute", (char) 250, "ucirc", (char) 251, "uuml", (char) 252, "yacute", (char) 253, "thorn", (char) 254, "yuml", (char) 255, "fnof", (char) 402, "Alpha",
      (char) 913, "Beta", (char) 914, "Gamma", (char) 915, "Delta", (char) 916, "Epsilon", (char) 917, "Zeta", (char) 918, "Eta", (char) 919, "Theta", (char) 920, "Iota", (char) 921, "Kappa", (char) 922, "Lambda", (char) 923, "Mu", (char) 924, "Nu",
      (char) 925, "Xi", (char) 926, "Omicron", (char) 927, "Pi", (char) 928, "Rho", (char) 929, "Sigma", (char) 931, "Tau", (char) 932, "Upsilon", (char) 933, "Phi", (char) 934, "Chi", (char) 935, "Psi", (char) 936, "Omega", (char) 937, "alpha",
      (char) 945, "beta", (char) 946, "gamma", (char) 947, "delta", (char) 948, "epsilon", (char) 949, "zeta", (char) 950, "eta", (char) 951, "theta", (char) 952, "iota", (char) 953, "kappa", (char) 954, "lambda", (char) 955, "mu", (char) 956, "nu",
      (char) 957, "xi", (char) 958, "omicron", (char) 959, "pi", (char) 960, "rho", (char) 961, "sigmaf", (char) 962, "sigma", (char) 963, "tau", (char) 964, "upsilon", (char) 965, "phi", (char) 966, "chi", (char) 967, "psi", (char) 968, "omega",
      (char) 969, "thetasym", (char) 977, "upsih", (char) 978, "piv", (char) 982, "bull", (char) 8226, "hellip", (char) 8230, "prime", (char) 8242, "Prime", (char) 8243, "oline", (char) 8254, "frasl", (char) 8260, "weierp", (char) 8472, "image",
      (char) 8465, "real", (char) 8476, "trade", (char) 8482, "alefsym", (char) 8501, "larr", (char) 8592, "uarr", (char) 8593, "rarr", (char) 8594, "darr", (char) 8595, "harr", (char) 8596, "crarr", (char) 8629, "lArr", (char) 8656, "uArr",
      (char) 8657, "rArr", (char) 8658, "dArr", (char) 8659, "hArr", (char) 8660, "forall", (char) 8704, "part", (char) 8706, "exist", (char) 8707, "empty", (char) 8709, "nabla", (char) 8711, "isin", (char) 8712, "notin", (char) 8713, "ni",
      (char) 8715, "prod", (char) 8719, "sum", (char) 8721, "minus", (char) 8722, "lowast", (char) 8727, "radic", (char) 8730, "prop", (char) 8733, "infin", (char) 8734, "ang", (char) 8736, "and", (char) 8743, "or", (char) 8744, "cap", (char) 8745,
      "cup", (char) 8746, "int", (char) 8747, "there4", (char) 8756, "sim", (char) 8764, "cong", (char) 8773, "asymp", (char) 8776, "ne", (char) 8800, "equiv", (char) 8801, "le", (char) 8804, "ge", (char) 8805, "sub", (char) 8834, "sup",
      (char) 8835, "nsub", (char) 8836, "sube", (char) 8838, "supe", (char) 8839, "oplus", (char) 8853, "otimes", (char) 8855, "perp", (char) 8869, "sdot", (char) 8901, "lceil", (char) 8968, "rceil", (char) 8969, "lfloor", (char) 8970, "rfloor",
      (char) 8971, "lang", (char) 9001, "rang", (char) 9002, "loz", (char) 9674, "spades", (char) 9824, "clubs", (char) 9827, "hearts", (char) 9829, "diams", (char) 9830, "quot", (char) 34, "amp", (char) 38, "lt", (char) 60, "gt", (char) 62,
      "OElig", (char) 338, "oelig", (char) 339, "Scaron", (char) 352, "scaron", (char) 353, "Yuml", (char) 376, "circ", (char) 710, "tilde", (char) 732, "ensp", (char) 8194, "emsp", (char) 8195, "thinsp", (char) 8201, "zwnj", (char) 8204, "zwj",
      (char) 8205, "lrm", (char) 8206, "rlm", (char) 8207, "ndash",
      (char) 8211, //0x2013
      "mdash", (char) 8212, "lsquo", (char) 8216, "rsquo", (char) 8217, "sbquo", (char) 8218, "ldquo", (char) 8220, "rdquo", (char) 8221, "bdquo", (char) 8222, "dagger", (char) 8224, "Dagger", (char) 8225, "permil", (char) 8240, "lsaquo",
      (char) 8249, "rsaquo", (char) 8250, "euro", (char) 8364, "apos", '\'');

  /** Maps characters to normalizations */
  public static Map<Character, String> normalizeMap = new TreeMap<Character, String>();
  static {
    Object[] o = new Object[] {
        // ASCII
        (char) 7,
        "BEEP",
        (char) 9,
        " ",
        (char) 10,
        "\n",

        // Latin-1
        (char) 160,
        " ",
        (char) 161,
        "!",
        (char) 162,
        "cent",
        (char) 163,
        "pound",
        (char) 164,
        "currency",
        (char) 165,
        "yen",
        (char) 166,
        "|",
        (char) 167,
        "/",
        (char) 169,
        "(c)",
        (char) 170,
        "^a",
        (char) 171,
        "\"",
        (char) 172,
        "~",
        (char) 173,
        "",
        (char) 174,
        "(R)",
        (char) 176,
        "degree",
        (char) 177,
        "+/-",
        (char) 178,
        "^2",
        (char) 179,
        "^3",
        (char) 180,
        "'",
        (char) 181,
        "mu",
        (char) 182,
        "P",
        (char) 183,
        ".",
        (char) 184,
        ",",
        (char) 185,
        "^1",
        (char) 186,
        "^o",
        (char) 187,
        "\"",
        (char) 188,
        "1/4",
        (char) 189,
        "1/2",
        (char) 190,
        "3/4",
        (char) 191,
        "?",
        (char) 0xC4,
        "Ae",
        (char) 0xD6,
        "Oe",
        (char) 0xDC,
        "Ue",
        (char) 0xDF,
        "ss",
        (char) 0xC6,
        "Ae",
        (char) 0xC7,
        "C",
        (char) 0xD0,
        "D",
        (char) 0xD1,
        "N",
        (char) 0xD7,
        "x",
        (char) 0xDD,
        "Y",
        (char) 0xDE,
        "b",
        (char) 0xF7,
        "/",
        (char) 0xFF,
        "y",

        // Latin Extended-A
        (char) 0x132,
        "IJ",
        (char) 0x134,
        "J",
        (char) 0x170,
        "Ue",
        (char) 0x174,
        "W",
        (char) 0x17F,
        "f",

        // Greek
        (char) 0x374,
        "'",
        (char) 0x375,
        ",",
        (char) 0x37A,
        ",",
        (char) 0x37E,
        ";",
        (char) 0x384,
        "'",
        (char) 0x385,
        "'",
        (char) 0x386,
        "A",
        (char) 0x387,
        ".",
        (char) 0x388,
        "E",
        (char) 0x380,
        "I",
        (char) 0x38C,
        "O",
        (char) 0x38E,
        "Y",
        (char) 0x38F,
        "O",
        (char) 0x390,
        "i",
        (char) 215,
        "*",
        (char) 913,
        "A",
        (char) 914,
        "B",
        (char) 915,
        "G",
        (char) 916,
        "D",
        (char) 917,
        "E",
        (char) 918,
        "Z",
        (char) 919,
        "E",
        (char) 920,
        "Th",
        (char) 921,
        "I",
        (char) 922,
        "K",
        (char) 923,
        "L",
        (char) 924,
        "M",
        (char) 925,
        "N",
        (char) 926,
        "X",
        (char) 927,
        "O",
        (char) 928,
        "P",
        (char) 929,
        "R",
        (char) 931,
        "S",
        (char) 932,
        "T",
        (char) 933,
        "Y",
        (char) 934,
        "Ph",
        (char) 935,
        "Ch",
        (char) 936,
        "Ps",
        (char) 937,
        "O",
        (char) 977,
        "th",
        (char) 978,
        "y",
        (char) 982,
        "pi",

        // General Punctuation
        (char) 0x2013,
        "-",
        (char) 0x2016,
        "||",
        (char) 0x2017,
        "_",
        (char) 0x2020,
        "+",
        (char) 0x2021,
        "++",
        (char) 0x2022,
        "*",
        (char) 0x2023,
        "*",
        (char) 0x2024,
        ".",
        (char) 0x2025,
        "..",
        (char) 0x2026,
        "...",
        (char) 0x2027,
        ".",
        (char) 0x2028,
        "\n",
        (char) 0x2030,
        "/1000",
        (char) 0x2031,
        "/10000",
        (char) 0x2032,
        "'",
        (char) 0x2033,
        "''",
        (char) 0x2034,
        "'''",
        (char) 0x2035,
        "'",
        (char) 0x2036,
        "''",
        (char) 0x2037,
        "'''",
        (char) 0x2038,
        "^",
        (char) 0x2039,
        "\"",
        (char) 0x203A,
        "\"",
        (char) 0x203B,
        "*",
        (char) 0x203C,
        "!!",
        (char) 0x203D,
        "?!",
        (char) 0x2041,
        ",",
        (char) 0x2042,
        "***",
        (char) 0x2043,
        "-",
        (char) 0x2044,
        "/",
        (char) 0x2045,
        "[",
        (char) 0x2046,
        "]",
        (char) 0x2047,
        "??",
        (char) 0x2048,
        "?!",
        (char) 0x2049,
        "!?",
        (char) 0x204A,
        "-",
        (char) 0x204B,
        "P",
        (char) 0x204C,
        "<",
        (char) 0x204D,
        ">",
        (char) 0x204F,
        ";",
        (char) 0x2050,
        "-",
        (char) 0x2051,
        "**",
        (char) 0x2052,
        "./.",
        (char) 0x2053,
        "~",
        (char) 0x2054,
        "_",
        (char) 0x2055,
        "_",

        // Mathematical symbols
        (char) 8465,
        "I",
        (char) 8476,
        "R",
        (char) 8482,
        "(TM)",
        (char) 8501,
        "a",
        (char) 8592,
        "<-",
        (char) 8593,
        "^",
        (char) 8594,
        "->",
        (char) 8595,
        "v",
        (char) 8596,
        "<->",
        (char) 8629,
        "<-'",
        (char) 8656,
        "<=",
        (char) 8657,
        "^",
        (char) 8658,
        "=>",
        (char) 8659,
        "v",
        (char) 8660,
        "<=>",
        (char) 8704,
        "FOR ALL",
        (char) 8706,
        "d",
        (char) 8707,
        "EXIST",
        (char) 8709,
        "{}",
        (char) 8712,
        "IN",
        (char) 8713,
        "NOT IN",
        (char) 8715,
        "CONTAINS",
        (char) 8719,
        "PRODUCT",
        (char) 8721,
        "SUM",
        (char) 8722,
        "-",
        (char) 8727,
        "*",
        (char) 8730,
        "SQRT",
        (char) 8733,
        "~",
        (char) 8734,
        "INF",
        (char) 8736,
        "angle",
        (char) 8743,
        "&",
        (char) 8744,
        "|",
        (char) 8745,
        "INTERSECTION",
        (char) 8746,
        "UNION",
        (char) 8747,
        "INTEGRAL",
        (char) 8756,
        "=>",
        (char) 8764,
        "~",
        (char) 8773,
        "~=",
        (char) 8776,
        "~=",
        (char) 8800,
        "!=",
        (char) 8801,
        "==",
        (char) 8804,
        "=<",
        (char) 8805,
        ">=",
        (char) 8834,
        "SUBSET OF",
        (char) 8835,
        "SUPERSET OF",
        (char) 8836,
        "NOT SUBSET OF",
        (char) 8838,
        "SUBSET OR EQUAL",
        (char) 8839,
        "SUPERSET OR EQUAL",
        (char) 8853,
        "(+)",
        (char) 8855,
        "(*)",
        (char) 8869,
        "_|_",
        (char) 8901,
        "*",
        (char) 8364,
        "EUR",

        // Ligatures
        (char) 0xFB00,
        "ff",
        (char) 0xFB01,
        "fi",
        (char) 0xFB02,
        "fl",
        (char) 0xFB03,
        "ffi",
        (char) 0xFB04,
        "ffl",
        (char) 0xFB05,
        "ft",
        (char) 0xFB06,
        "st" };
    for (int i = 0; i < o.length; i += 2)
      normalizeMap.put((Character) o[i], (String) o[i + 1]);
  }

  /** Normalizes a character to a String of characters in the range 0x20-0x7F.
   *  Returns a String, because some characters are
    * normalized to multiple characters (e.g. umlauts) and
    * some characters are normalized to zero characters (e.g. special Unicode space chars).
    * Returns null for the EndOfFile character -1 */
  public static String normalize(int c) {
    // EOF
    if (c == -1) return (null);

    // ASCII chars
    if (c >= ' ' && c <= 128) return ("" + (char) c);

    // Upper case
    boolean u = Character.isUpperCase(c);
    char cu = (char) Character.toUpperCase(c);

    // Check map
    if (normalizeMap.get(cu) != null) return (u ? normalizeMap.get(cu) : normalizeMap.get(cu).toLowerCase());

    // ASCII
    if (c < ' ') return ("");

    // Latin-1
    if (cu >= 0xC0 && cu <= 0xC5) return (u ? "A" : "a");
    if (cu >= 0xC8 && cu <= 0xCB) return (u ? "E" : "e");
    if (cu >= 0xCC && cu <= 0xCF) return (u ? "I" : "i");
    if (cu >= 0xD2 && cu <= 0xD8) return (u ? "O" : "o");
    if (cu >= 0x80 && cu <= 0xA0) return (" ");

    //  Latin Extended-A
    if (cu >= 0x100 && cu <= 0x105) return (u ? "A" : "a");
    if (cu >= 0x106 && cu <= 0x10D) return (u ? "C" : "c");
    if (cu >= 0x10E && cu <= 0x111) return (u ? "D" : "d");
    if (cu >= 0x112 && cu <= 0x11B) return (u ? "E" : "e");
    if (cu >= 0x11C && cu <= 0x123) return (u ? "G" : "g");
    if (cu >= 0x124 && cu <= 0x127) return (u ? "H" : "h");
    if (cu >= 0x128 && cu <= 0x131) return (u ? "I" : "i");
    if (cu >= 0x136 && cu <= 0x138) return (u ? "K" : "k");
    if (cu >= 0x139 && cu <= 0x142) return (u ? "L" : "l");
    if (cu >= 0x143 && cu <= 0x14B) return (u ? "N" : "n");
    if (cu >= 0x14C && cu <= 0x14F) return (u ? "O" : "o");
    if (cu >= 0x150 && cu <= 0x153) return (u ? "Oe" : "oe");
    if (cu >= 0x156 && cu <= 0x159) return (u ? "R" : "r");
    if (cu >= 0x15A && cu <= 0x161) return (u ? "S" : "s");
    if (cu >= 0x161 && cu <= 0x167) return (u ? "T" : "t");
    if (cu >= 0x176 && cu <= 0x178) return (u ? "Y" : "y");
    if (cu >= 0x179 && cu <= 0x17E) return (u ? "Z" : "z");

    // General Punctuation
    if (cu >= 0x2000 && cu <= 0x200A) return (" ");
    if (cu >= 0x200B && cu <= 0x200F) return ("");
    if (cu >= 0x2010 && cu <= 0x2015) return ("--");
    if (cu >= 0x2018 && cu <= 0x201B) return ("'");
    if (cu >= 0x201C && cu <= 0x201F) return ("\"");
    if (cu >= 0x2029 && cu <= 0x202F) return (" ");
    if (cu >= 0x203E && cu <= 0x2040) return ("-");
    if (cu >= 0x2056 && cu <= 0x205E) return (".");

    return (defaultNormalizer.apply((char) c));
  }

  /** Eats a String of the form "%xx" from a string, where
   * xx is a hexadecimal code. If xx is a UTF8 code start, 
   * tries to complete the UTF8-code and decodes it.*/
  public static char eatPercentage(String a, int[] n) {
    // Length 0
    if (!a.startsWith("%") || a.length() < 3) {
      n[0] = 0;
      return ((char) 0);
    }
    char c;
    // Try to parse first char
    try {
      c = (char) Integer.parseInt(a.substring(1, 3), 16);
    } catch (Exception e) {
      n[0] = -1;
      return ((char) 0);
    }
    // For non-UTF8, return the char    
    int len = Utf8Length(c);
    n[0] = 3;
    if (len <= 1) return (c);
    // Else collect the UTF8
    String dec = "" + c;
    for (int i = 1; i < len; i++) {
      try {
        dec += (char) Integer.parseInt(a.substring(1 + i * 3, 3 + i * 3), 16);
      } catch (Exception e) {
        return (c);
      }
    }
    // Try to decode the UTF8
    int[] eatLength = new int[1];
    char utf8 = eatUtf8(dec, eatLength);
    if (eatLength[0] != len) return (c);
    n[0] = len * 3;
    return (utf8);
  }

  /** Eats an HTML ampersand code from a String*/
  public static char eatAmpersand(String a, int[] n) {
    n[0] = 0;
    if (!a.startsWith("&")) return ((char) 0);
    // Seek to ';'
    // We also accept spaces and the end of the String as a delimiter
    while (n[0] < a.length() && !Character.isSpaceChar(a.charAt(n[0])) && a.charAt(n[0]) != ';')
      n[0]++;
    if (n[0] <= 1) {
      n[0] = -1;
      return ((char) 0);
    }
    if (n[0] < a.length() && a.charAt(n[0]) == ';') {
      a = a.substring(1, n[0]);
      n[0]++;
    } else {
      a = a.substring(1, n[0]);
    }
    // Hexadecimal characters
    if (a.startsWith("#x")) {
      try {
        return ((char) Integer.parseInt(a.substring(2), 16));
      } catch (Exception e) {
        n[0] = -1;
        return ((char) 0);
      }
    }
    // Decimal characters
    if (a.startsWith("#")) {
      try {
        return ((char) Integer.parseInt(a.substring(1)));
      } catch (Exception e) {
        n[0] = -1;
        return ((char) 0);
      }
    }
    // Others
    if (ampersandMap.get(a) != null) return (ampersandMap.get(a));
    else if (ampersandMap.get(a.toLowerCase()) != null) return (ampersandMap.get(a.toLowerCase()));
    n[0] = -1;
    return ((char) 0);
  }

  /** Tells from the first UTF-8 code character how long the code is.
   * Returns -1 if the character is not an UTF-8 code start.
   * Returns 1 if the character is ASCII<128*/
  public static int Utf8Length(char c) {
    // 0xxx xxxx
    if ((c & 0x80) == 0x00) return (1);
    // 110x xxxx
    if ((c & 0xE0) == 0xC0) return (2);
    // 1110 xxxx
    if ((c & 0xF0) == 0xE0) return (3);
    // 1111 0xxx
    if ((c & 0xF8) == 0xF0) return (4);
    return (-1);
  }

  /** Eats a UTF8 code from a String. There is also a built-in way in Java that converts
   * UTF8 to characters and back, but it does not work with all characters. */
  public static char eatUtf8(String a, int[] n) {
    if (a.length() == 0) {
      n[0] = 0;
      return ((char) 0);
    }
    n[0] = Utf8Length(a.charAt(0));
    if (a.length() >= n[0]) {
      switch (n[0]) {
        case 1:
          return (a.charAt(0));
        case 2:
          if ((a.charAt(1) & 0xC0) != 0x80) break;
          return ((char) (((a.charAt(0) & 0x1F) << 6) + (a.charAt(1) & 0x3F)));
        case 3:
          if ((a.charAt(1) & 0xC0) != 0x80 || (a.charAt(2) & 0xC0) != 0x80) break;
          return ((char) (((a.charAt(0) & 0x0F) << 12) + ((a.charAt(1) & 0x3F) << 6) + ((a.charAt(2) & 0x3F))));
        case 4:
          if ((a.charAt(1) & 0xC0) != 0x80 || (a.charAt(2) & 0xC0) != 0x80 || (a.charAt(3) & 0xC0) != 0x80) break;
          return ((char) (((a.charAt(0) & 0x07) << 18) + ((a.charAt(1) & 0x3F) << 12) + ((a.charAt(2) & 0x3F) << 6) + ((a.charAt(3) & 0x3F))));
      }
    }
    n[0] = -1;
    return ((char) 0);
  }

  /** Decodes all UTF8 characters in the string*/
  public static String decodeUTF8(String s) {
    StringBuilder result = new StringBuilder();
    int[] eatLength = new int[1];
    while (s.length() != 0) {
      char c = eatUtf8(s, eatLength);
      if (eatLength[0] != -1) {
        result.append(c);
        s = s.substring(eatLength[0]);
      } else {
        result.append(s.charAt(0));
        s = s.substring(1);
      }
    }
    return (result.toString());
  }

  /** Decodes all percentage characters in the string*/
  public static String decodePercentage(String s) {
    StringBuilder result = new StringBuilder();
    int[] eatLength = new int[1];
    while (s.length() != 0) {
      char c = eatPercentage(s, eatLength);
      if (eatLength[0] > 1) {
        result.append(c);
        s = s.substring(eatLength[0]);
      } else {
        result.append(s.charAt(0));
        s = s.substring(1);
      }
    }
    return (result.toString());
  }

  /** Fabian: This method cannot decode numeric hexadecimal ampersand codes. What is its purpose? TODO*/
  public static String decodeAmpersand_UNKNOWN(String s) {
    if (s == null) {
      return null;
    }
    StringBuffer sb = new StringBuffer(s.length());
    while (s != null && s.length() != 0) {
      int i = s.indexOf("&");
      if (i == -1) {
        sb.append(s);
        s = null;
      } else {
        boolean space = false;
        boolean end = false;
        sb.append(s.substring(0, i));
        s = s.substring(i);
        int j1 = s.indexOf(";");
        int j2 = s.indexOf(" ");
        int j = -1;
        if (j1 == -1 || j2 == -1) {
          if (j1 == -1 && j2 == -1) {
            end = true;
            j = s.length();
          } else if (j1 == -1) {
            j = j2;
          } else if (j2 == -1) {
            j = j1;
          }
        } else if (j1 < j2) {
          j = j1;
        } else if (j1 > j2) {
          j = j2;
          space = true;
        }
        String a = s.substring(1, j);
        if (ampersandMap.get(a) != null) {
          sb.append(ampersandMap.get(a));
          if (space) {
            sb.append(' ');
          }
        } else if (a.startsWith("#")) {
          try {
            sb.append(((char) Integer.parseInt(a.substring(1))));
          } catch (Exception e) {
            sb.append(a);
          }
          if (space) {
            sb.append(' ');
          }
        } else {
          if (end) {
            sb.append(s.substring(0, j));
          } else {
            sb.append(s.substring(0, j + 1));
          }
        }
        if (end) {
          s = s.substring(j);
        } else {
          s = s.substring(j + 1);
        }
      }
    }
    return sb.toString();
  }

  public static String decodeAmpersand(String s, PositionTracker posTracker) {
    if (s == null) {
      return null;
    }
    int pos = 0;
    int difference;
    StringBuffer sb = new StringBuffer(s.length());
    while (s != null && s.length() != 0) {
      int i = s.indexOf("&");
      if (i == -1) {
        sb.append(s);
        s = null;
      } else {
        boolean space = false;
        boolean end = false;
        sb.append(s.substring(0, i));
        s = s.substring(i);
        pos += i;
        int j1 = s.indexOf(";");
        int j2 = s.indexOf(" ");
        int j = -1;
        if (j1 == -1 || j2 == -1) {
          if (j1 == -1 && j2 == -1) {
            end = true;
            j = s.length();
          } else if (j1 == -1) {
            j = j2;
          } else if (j2 == -1) {
            j = j1;
          }
        } else if (j1 < j2) {
          j = j1;
        } else if (j1 > j2) {
          j = j2;
          space = true;
        }
        pos += (j + 1);
        String a = s.substring(1, j);
        if (ampersandMap.get(a) != null) {
          sb.append(ampersandMap.get(a));
          difference = 1 - (j + 1);
          if (space) {
            sb.append(' ');
            difference++;
          }
          posTracker.addPositionChange(pos, difference);
        } else {
          if (end) {
            sb.append(s.substring(0, j));
          } else {
            sb.append(s.substring(0, j + 1));
          }
        }
        if (end) {
          s = s.substring(j);
        } else {
          s = s.substring(j + 1);
        }
      }
    }
    posTracker.closeRun();
    return sb.toString();
  }

  /** Decodes all ampersand sequences in the string*/
  public static String decodeAmpersand(String s) {
    if(s==null || s.indexOf('&')==-1) return(s);
    StringBuilder result = new StringBuilder();
    int[] eatLength = new int[1];// add this in order to multithread safe
    while (s.length() != 0) {
      char c = eatAmpersand(s, eatLength);
      if (eatLength[0] > 1) {
        result.append(c);
        s = s.substring(eatLength[0]);
      } else {
        result.append(s.charAt(0));
        s = s.substring(1);
      }
    }
    return (result.toString());
  }

  /** Decodes all backslash characters in the string */
  public static String decodeBackslash(String s) {
	  if(s==null || s.indexOf('\\')==-1) return(s);
    StringBuilder result = new StringBuilder();
    int[] eatLength = new int[1];
    while (s.length() != 0) {
      char c = eatBackslash(s, eatLength);
      if (eatLength[0] > 1) {
        result.append(c);
        s = s.substring(eatLength[0]);
      } else {
        result.append(s.charAt(0));
        s = s.substring(1);
      }
    }
    return (result.toString());
  }

  /** Used for encoding selected characters*/
  public static interface Legal {
	  public boolean isLegal(char c);
  }
  
  /** Encodes with backslash all illegal characters*/
  public static String encodeBackslash(CharSequence s, Legal legal) {
	StringBuilder b=new StringBuilder((int)(s.length()*1.5));
	for(int i=0;i<s.length();i++) {
		if(legal.isLegal(s.charAt(i))) {
			b.append(s.charAt(i));
		} else {
			if(charToBackslash.containsKey(s.charAt(i))) {
				b.append(charToBackslash.get(s.charAt(i)));
				continue;
			}
			b.append("\\u");
		    String hex = Integer.toHexString(s.charAt(i));
		    for(int j=0;j<4-hex.length();j++)
		      b.append('0');
		    b.append(hex);
		}
	}
	return(b.toString());
  }
  
  /** Eats a backslash sequence from a String */
  public static char eatBackslash(String a, int[] n) {
    if (!a.startsWith("\\")) {
      n[0] = 0;
      return ((char) 0);
    }
    // Unicodes BS u XXXX
    if (a.startsWith("\\u")) {
      try {
        n[0] = 6;
        return ((char) Integer.parseInt(a.substring(2, 6), 16));
      } catch (Exception e) {
        n[0] = -1;
        return ((char) 0);
      }
    }
    // Unicodes BS uu XXXX
    if (a.startsWith("\\uu")) {
      try {
        n[0] = 7;
        return ((char) Integer.parseInt(a.substring(3, 7), 16));
      } catch (Exception e) {
        n[0] = -1;
        return ((char) 0);
      }
    }
    // Classical escape sequences
    if (a.startsWith("\\b")) {
      n[0] = 2;
      return ((char) 8);
    }
    if (a.startsWith("\\t")) {
      n[0] = 2;
      return ((char) 9);
    }
    if (a.startsWith("\\n")) {
      n[0] = 2;
      return ((char) 10);
    }
    if (a.startsWith("\\f")) {
      n[0] = 2;
      return ((char) 12);
    }
    if (a.startsWith("\\r")) {
      n[0] = 2;
      return ((char) 13);
    }
    if (a.startsWith("\\\\")) {
      n[0] = 2;
      return ('\\');
    }
    if (a.startsWith("\\\"")) {
      n[0] = 2;
      return ('"');
    }
    if (a.startsWith("\\'")) {
      n[0] = 2;
      return ('\'');
    }
    // Octal codes
    n[0] = 1;
    while (n[0] < a.length() && a.charAt(n[0]) >= '0' && a.charAt(n[0]) <= '8')
      n[0]++;
    if (n[0] == 1) {
      n[0] = 0;
      return ((char) 0);
    }
    try {
      return ((char) Integer.parseInt(a.substring(1, n[0]), 8));
    } catch (Exception e) {
    }
    n[0] = -1;
    return ((char) 0);
  }

  /** Replaces all codes in a String by the 16 bit Unicode characters */
  public static String decode(String s) {
    StringBuilder b = new StringBuilder();
    int[] eatLength = new int[1];
    while (s.length() > 0) {
      char c = eatPercentage(s, eatLength);
      if (eatLength[0] <= 0) {
        c = eatAmpersand(s, eatLength);
        if (eatLength[0] <= 0) {
          c = eatBackslash(s, eatLength);
          if (eatLength[0] <= 0) {
            c = eatUtf8(s, eatLength);
            if (eatLength[0] <= 0) {
              c = s.charAt(0);
              eatLength[0] = 1;
            }
          }
        }
      }
      b.append(c);
      s = s.substring(eatLength[0]);
    }
    return (b.toString());
  }

  /** Encodes a character to UTF8 (if necessary)*/
  public static String encodeUTF8(int c) {
    if (c <= 0x7F) return ("" + (char) c);
    if (c <= 0x7FF) return ("" + (char) (0xC0 + ((c >> 6) & 0x1F)) + (char) (0x80 + (c & 0x3F)));
    if (c <= 0xFFFF) return ("" + (char) (0xE0 + ((c >> 12) & 0x0F)) + (char) (0x80 + ((c >> 6) & 0x3F)) + (char) (0x80 + (c & 0x3F)));
    return ("" + c);
  }

  /** Encodes a character to a backslash code (if necessary)*/
  public static String encodeBackslash(char c) {
    if (isAlphanumeric(c) || c == ' ') return ("" + c);
    String hex = Integer.toHexString(c);
    while (hex.length() < 4)
      hex = "0" + hex;
    return ("\\u" + hex);
  }

  /** Encodes a character to a backslash code (if not alphanumeric)*/
  public static String encodeBackslashToAlphanumeric(char c) {
    if (isAlphanumeric(c) || c == '_') return ("" + c);
    String hex = Integer.toHexString(c);
    while (hex.length() < 4)
      hex = "0" + hex;
    return ("\\u" + hex);
  }

  /** Encodes a character to a backslash code (if not ASCII)*/
  public static String encodeBackslashToASCII(char c) {
    if (c >= 32 && c < 128 && c != '\\' && c != '"') return ("" + c);
    String hex = Integer.toHexString(c);
    while (hex.length() < 4)
      hex = "0" + hex;
    return ("\\u" + hex);
  }

  /** Encodes a character to an HTML-Ampersand code (if necessary)*/
  public static String encodeAmpersand(char c) {
    String s;
    if (null != (s = charToAmpersand.get(c))) return (s);
    if (c < 128 && c >= 32) return ("" + c);
    else return ("&#" + ((int) c) + ";");
  }

  /** Encodes a character to an HTML-Ampersand code (if necessary)*/
  public static String encodeAmpersandToAlphanumeric(char c) {
    if (isAlphanumeric(c) || c == '_') return ("" + c);
    return ("&#" + ((int) c) + ";");
  }

  /** Encodes a character to an Percentage code (if necessary).
   * If the character is greater than 0x80, the character is converted to
   * a UTF8-sequence and this sequence is encoded as percentage codes. */
  public static String encodePercentage(char c) {
    if (isAlphanumeric(c)) return ("" + c);
    if (c < 16) return ("%0" + Integer.toHexString(c).toUpperCase());
    if (c < 128) return ("%" + Integer.toHexString(c).toUpperCase());
    String s = encodeUTF8(c);
    String result = "";
    for (int i = 0; i < s.length(); i++) {
      result += "%" + Integer.toHexString(s.charAt(i)).toUpperCase();
    }
    return (result);
  }

  /**
   * Encodes a String with reserved XML characters into a valid xml string for attributes.  
   * @param str
   * @return 
   */
  public static String encodeXmlAttribute(String str) {
    if (str == null) return null;
    int len = str.length();
    if (len == 0) return str;
    StringBuffer encoded = new StringBuffer();
    for (int i = 0; i < len; i++) {
      char c = str.charAt(i);
      if (c == '<') encoded.append("&lt;");
      else if (c == '\"') encoded.append("&quot;");
      else if (c == '>') encoded.append("&gt;");
      else if (c == '\'') encoded.append("&apos;");
      else if (c == '&') encoded.append("&amp;");
      else encoded.append(c);
    }
    return encoded.toString();
  }

  /** Tells whether a char is in a range*/
  public static boolean in(char c, char a, char b) {
    return (c >= a && c <= b);
  }

  /** Tells whether a char is in a string*/
  public static boolean in(char c, String s) {
    return (s.indexOf(c) != -1);
  }

  /** Tells whether a char is alphanumeric in the sense of URIs*/
  public static boolean isAlphanumeric(char c) {
    return (in(c, 'a', 'z') || in(c, 'A', 'Z') || in(c, '0', '9'));
  }

  /** Tells whether a char is reserved in the sense of URIs*/
  public static boolean isReserved(char c) {
    return (isSubDelim(c) || isGenDelim(c));
  }

  /** Tells whether a char is unreserved in the sense of URIs (not the same as !reserved)*/
  public static boolean isUnreserved(char c) {
    return (isAlphanumeric(c) || in(c, "-._~"));
  }

  /** Tells whether a string is escaped in the sense of URIs*/
  public static boolean isEscaped(String s) {
    return (s.matches("%[0-9A-Fa-f]{2}"));
  }

  /** Tells whether a char is a sub-delimiter in the sense of URIs*/
  public static boolean isSubDelim(char c) {
    return (in(c, "!$&'()*+,="));
  }

  /** Tells whether a char is a general delimiter in the sense of URIs*/
  public static boolean isGenDelim(char c) {
    return (in(c, ":/?#[]@"));
  }

  /** Tells whether a char is a valid path component in the sense of URIs*/
  public static boolean isPchar(char c) {
    return (isUnreserved(c) || isSubDelim(c) || in(c, "@"));
  }

  /** Encodes a char to percentage code, if it is not a path character in the sense of URIs*/
  public static String encodeURIPathComponent(char c) {
    if (isPchar(c)) return ("" + c);
    else return (Char.encodePercentage(c));
  }

  /** Encodes a char to percentage code, if it is not a path character in the sense of URIs*/
  public static String encodeURIPathComponent(String s) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      result.append(Char.encodeURIPathComponent(s.charAt(i)));
    }
    return (result.toString());
  }

  /** Encodes a char to percentage code, if it is not a path character in the sense of XMLs*/
  public static String encodeURIPathComponentXML(String s) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == '&') result.append(Char.encodePercentage(s.charAt(i)));
      else if (s.charAt(i) == '"') result.append(Char.encodePercentage(s.charAt(i)));
      else result.append(Char.encodeURIPathComponent(s.charAt(i)));
    }
    return (result.toString());
  }

  /** Decodes a URI path component*/
  public static String decodeURIPathComponent(String s) {
    return (Char.decodePercentage(s));
  }

  /** Encodes a String to UTF8 */
  public static String encodeUTF8(String c) {
    StringBuilder r = new StringBuilder();
    for (int i = 0; i < c.length(); i++) {
      r.append(encodeUTF8(c.charAt(i)));
    }
    return (r.toString());
  }

  /** Replaces non-normal characters in a String by Backslash codes */
  public static String encodeBackslash(String c) {
    StringBuilder r = new StringBuilder();
    for (int i = 0; i < c.length(); i++) {
      r.append(encodeBackslash(c.charAt(i)));
    }
    return (r.toString());
  }

  /** Replaces non-normal characters in a String by Backslash codes (if not alphanumeric)*/
  public static String encodeBackslashToAlphanumeric(String c) {
    StringBuilder r = new StringBuilder();
    for (int i = 0; i < c.length(); i++) {
      r.append(encodeBackslashToAlphanumeric(c.charAt(i)));
    }
    return (r.toString());
  }

  /** Replaces non-normal characters in a String by Backslash codes (if not ASCII)*/
  public static String encodeBackslashToASCII(String c) {
    StringBuilder r = new StringBuilder();
    for (int i = 0; i < c.length(); i++) {
      r.append(encodeBackslashToASCII(c.charAt(i)));
    }
    return (r.toString());
  }

  /** Replaces non-normal characters in a String by HTML Ampersand codes */
  public static String encodeAmpersand(String c) {
    StringBuilder r = new StringBuilder();
    for (int i = 0; i < c.length(); i++) {
      r.append(encodeAmpersand(c.charAt(i)));
    }
    return (r.toString());
  }

  /** Replaces non-normal characters in a String by HTML Ampersand codes */
  public static String encodeAmpersandToAlphanumeric(String c) {
    StringBuilder r = new StringBuilder();
    for (int i = 0; i < c.length(); i++) {
      r.append(encodeAmpersandToAlphanumeric(c.charAt(i)));
    }
    return (r.toString());
  }

  /** Replaces non-normal characters in a String by Percentage codes.
   * If a character is greater than 0x80, the character is converted to
   * a UTF8-sequence and this sequence is encoded as percentage codes. */
  public static String encodePercentage(String c) {
    StringBuilder r = new StringBuilder();
    for (int i = 0; i < c.length(); i++) {
      r.append(encodePercentage(c.charAt(i)));
    }
    return (r.toString());
  }

  /** Decodes all codes in a String and normalizes all chars */
  public static String decodeAndNormalize(String s) {
    return (normalize(decode(s)));
  }

  /** Normalizes all chars in a String to characters 0x20-0x7F */
  public static String normalize(String s) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < s.length(); i++)
      b.append(normalize(s.charAt(i)));
    return (b.toString());
  }

  /** Returns the last character of a String or 0*/
  public static char last(CharSequence s) {
    return (s.length() == 0 ? (char) 0 : s.charAt(s.length() - 1));
  }

  /** Returns the String without the last character */
  public static String cutLast(String s) {
    return (s.length() == 0 ? "" : s.substring(0, s.length() - 1));
  }

  /** Cuts the last character */
  public static StringBuilder cutLast(StringBuilder s) {
    s.setLength(s.length() - 1);
    return (s);
  }

  /** Returns an HTML-String of the String */
  public static String toHTML(String s) {
    return (Char.encodeAmpersand(s).replace("&#10;", "<BR>"));
  }

  /** Returns the chars of a String in hex */
  public static String hexAll(String s) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      result.append(Integer.toHexString(s.charAt(i)).toUpperCase()).append(' ');
    }
    return (result.toString());
  }

  /** Replaces special characters in the string by hex codes (cannot be undone)*/
  public static String encodeHex(String s) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (isAlphanumeric(c)) result.append(c);
      else result.append(Integer.toHexString(s.charAt(i)).toUpperCase());
    }
    return (result.toString());
  }

  /** Upcases the first character in a String*/
  public static String upCaseFirst(String s) {
    if (s == null || s.length() == 0) return (s);
    return (Character.toUpperCase(s.charAt(0)) + s.substring(1));
  }

  /** Lowcases the first character in a String*/
  public static String lowCaseFirst(String s) {
    if (s == null || s.length() == 0) return (s);
    return (Character.toLowerCase(s.charAt(0)) + s.substring(1));
  }

  /** Returns a string of the given length, fills with spaces if necessary */
  public static CharSequence truncate(CharSequence s, int len) {
    if (s.length() == len) return (s);
    if (s.length() > len) return (s.subSequence(0, len));
    StringBuilder result = new StringBuilder(s);
    while (result.length() < len)
      result.append(' ');
    return (result);
  }

  /** Capitalizes words and lowercases the rest*/
  public static String capitalize(String s) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (i == 0 || i > 0 && !Character.isLetterOrDigit(s.charAt(i - 1))) c = Character.toUpperCase(c);
      else c = Character.toLowerCase(c);
      result.append(c);
    }
    return (result.toString());
  }

  /** TRUE if the Charsequence ends with the string */
  public static boolean endsWith(CharSequence s, String end) {
    return (s.length() >= end.length() && s.subSequence(s.length() - end.length(), s.length()).equals(end));
  }

  /** Test routine */
  public static void main(String argv[]) throws Exception {
    System.out.println("Enter a string with HTML ampersand codes, umlauts and/or UTF-8 codes and hit ENTER.");
    System.out.println("Press CTRL+C to abort");
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      String s = in.readLine();
      System.out.println("Decoded: " + (s = decode(s)));
      System.out.println("Normalized: " + normalize(s));
      System.out.println("As UTF8: " + encodeUTF8(s));
      System.out.println("As percentage: " + encodePercentage(s));
      System.out.println("As backslash: " + encodeBackslash(s));
      System.out.println("As ampersand: " + encodeAmpersand(s));
      System.out.println("As URI component: " + encodeURIPathComponent(s));
    }
  }
}
