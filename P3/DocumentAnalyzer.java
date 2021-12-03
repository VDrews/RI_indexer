package P3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Paths;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.store.Directory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.Link;
import org.apache.tika.sax.LinkContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;

import org.apache.lucene.analysis.core.*;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.commongrams.CommonGramsFilter;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.hunspell.Dictionary;
import org.apache.lucene.analysis.hunspell.HunspellStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.StopFilter;

public class DocumentAnalyzer {
  private String nombre;
  private String contenido;
  private Metadata metadata; // cambio el atributo de tipo a Metadata, para facilitar el acceso a los
                             // valores.
  private List<Link> enlaces;
  private LanguageResult language;

  String getNombre() {
    return nombre;
  }

  LanguageResult getLanguageResult() {
    return language;
  }

  List<Link> getEnlaces() {
    return enlaces;
  }

  Metadata getMetadata() {
    return metadata;
  }

  String getContenido() {
    return contenido;
  }

  DocumentAnalyzer(File file) throws Exception {
    this.nombre = file.getName();
    FileInputStream inputStream = new FileInputStream(file); // creamos el inputstream
    BodyContentHandler contentHandler = new BodyContentHandler(-1);
    this.metadata = new Metadata();
    ParseContext parser = new ParseContext();
    LinkContentHandler linkContentHandler = new LinkContentHandler();
    TeeContentHandler teeContentHandler = new TeeContentHandler(linkContentHandler, contentHandler);
    AutoDetectParser autodetectParser = new AutoDetectParser();

    autodetectParser.parse(inputStream, teeContentHandler, metadata, parser);
    LanguageDetector identifier = new OptimaizeLangDetector().loadModels();
    this.contenido = contentHandler.toString();
    this.language = identifier.detect(this.contenido);
    this.enlaces = linkContentHandler.getLinks();
  }

  private List<Entry<String, Integer>> hashMapToSortedArray(Map<String, Integer> map) {
    Set<Entry<String, Integer>> entries = map.entrySet();
    Comparator<Entry<String, Integer>> valueComparator = new Comparator<Entry<String, Integer>>() {
      @Override
      public int compare(Entry<String, Integer> e1, Entry<String, Integer> e2) {
        Integer v1 = e1.getValue();
        Integer v2 = e2.getValue();
        return v2.compareTo(v1);
      }
    };

    List<Entry<String, Integer>> orderedList = new ArrayList<Entry<String, Integer>>(entries);

    Collections.sort(orderedList, valueComparator);

    return orderedList;
  }

  private CharArraySet getStopWords() throws Exception {
    File stopWordsFile = new File("P2/dictionaries/stopwords.txt");
    DocumentAnalyzer stopWordsDocumentAnalyzer = new DocumentAnalyzer(stopWordsFile);
    Collection<String> stopWordsCollection = Arrays.asList(stopWordsDocumentAnalyzer.getContenido().split("\\r?\\n"));
    CharArraySet stopWords = new CharArraySet(stopWordsCollection, false);
    return stopWords;
  }

  private CharArraySet getCommonWords()throws Exception{
    File commonWordsFile = new File("P2/dictionaries/commonWords.txt");
    DocumentAnalyzer commondWordsDocumentAnalyzer = new DocumentAnalyzer(commonWordsFile);
    Collection<String> commondWordsCollection = Arrays.asList(commondWordsDocumentAnalyzer.getContenido().split("\\r?\\n"));
    CharArraySet commondWords = new CharArraySet(commondWordsCollection, true); // true para no distinguir entre mayusculas y minusculas
    return commondWords;
  }

   //Método que construye map de sinonimos.
  private static SynonymMap getSynonymMap() throws IOException {
    SynonymMap.Builder builder = new SynonymMap.Builder(true);
    builder.add(new CharsRef("ciencia"), new CharsRef("tecnología"), true);
    builder.add(new CharsRef("interdisciplinario"), new CharsRef("multidisciplinario"), true);
    builder.add(new CharsRef("investigadores"), new CharsRef("cientificos"), true);
    builder.add(new CharsRef("diferentes"), new CharsRef("distintos"), true);
    builder.add(new CharsRef("metodo"), new CharsRef("procedimiento"), true);

    return builder.build();
} 

  public List<Entry<String, Integer>> contador() throws IOException, TikaException {
    String[] parts = this.contenido.split(" ");
    Map<String, Integer> map = new HashMap<String, Integer>();
    for (String w : parts) {
      final String word = w.toLowerCase();
      Integer n = map.get(word);
      n = (n == null) ? 1 : ++n;
      if (Pattern.matches("[a-zA-Z\\u00C0-\\u024F\\u1E00-\\u1EFF]+", word))
        map.put(word, n);
    }

    return hashMapToSortedArray(map);
  }

  public List<Entry<String, Integer>> contador(String analyzerType) throws Exception {

    Analyzer analyzer;
    switch (analyzerType) {
      case "whiteAnalyzer":
        analyzer = new WhitespaceAnalyzer();
        break;
      case "simpleAnalyzer":
        analyzer = new SimpleAnalyzer();
        break;
      case "stopAnalyzer":
        analyzer = new StopAnalyzer(getStopWords());
        break;
      case "spanishAnalyzer":
        analyzer = new SpanishAnalyzer();
        break;
      case "customAnalyzer":
        analyzer = new Analyzer() {
          @Override
          protected TokenStreamComponents createComponents(String fieldName) {
            try {

              // Importamos los diccionarios
              InputStream affixStream = new FileInputStream("P2/dictionaries/es.aff");
              InputStream dictStream = new FileInputStream("P2/dictionaries/es.dic");

              // Carpeta temporal para el diccionario
              Directory directorioTemp = FSDirectory.open(Paths.get("P2/temp"));
              Dictionary dic = new Dictionary(directorioTemp, "temporalFile", affixStream, dictStream);

              // Tokenización
              Tokenizer source = new UAX29URLEmailTokenizer();

              // Filtramos las palabras vacías
              TokenStream result = new StopFilter(source, getStopWords());

              result = new HunspellStemFilter(result, dic, true, true);

              return new TokenStreamComponents(source, result);
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }
        };
        break;
      default:
        analyzer = new StandardAnalyzer(getStopWords());
        break;
    }

    TokenStream stream = analyzer.tokenStream(null, new StringReader(contenido));
    CharTermAttribute cAtt = stream.getAttribute(CharTermAttribute.class);

    stream.reset();

    ArrayList<String> parts = new ArrayList<String>();
    while (stream.incrementToken()) {
      parts.add(cAtt.toString());
    }
    stream.end();
    analyzer.close();

    Map<String, Integer> map = new HashMap<String, Integer>();
    for (String w : parts) {
      final String word = w.toLowerCase();
      Integer n = map.get(word);
      n = (n == null) ? 1 : ++n;
      if (Pattern.matches("[a-zA-Z\\u00C0-\\u024F\\u1E00-\\u1EFF]+", word))
        map.put(word, n);
    }

    return hashMapToSortedArray(map);
  }

  // Método que aplica un tokenizador estandar o custom y que aplica los
  // diferentes filtros.
  // Generando las diferentes salidas con ayuda de la clase OutputHelper.

  //Método que aplica un tokenizador estandar o custom y que aplica los diferentes filtros.  
  //Generando las diferentes salidas con ayuda de la clase OutputHelper.

  public List<String> applyDifferentFilter(int i) throws IOException{
    Analyzer analyzer;
    String filterName = new String();
    switch (i){
      //StandardFilter
      case 1:
      filterName="StandardFilter";
      analyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
          try {
            // Tokenización
            Tokenizer source = new UAX29URLEmailTokenizer();
    
            // Filtramos con  el filtro standard
            TokenStream std_filter = new StandardFilter(source);
    
            return new TokenStreamComponents(source, std_filter);
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }
        };
      break;

      //LowerCaseFilter
      case 2:
      filterName="LowerCaseFilter";
      analyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
          try {  
            // Tokenización
            Tokenizer source = new UAX29URLEmailTokenizer();
    
            // Filtramos las mayusculas
            TokenStream low_case_filter = new LowerCaseFilter(source);
    
            return new TokenStreamComponents(source, low_case_filter);
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }
        };

      break;
      //StopWordsFilter
      case 3:
      filterName="StopWordsFilter";
      analyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
          try {    
            // Tokenización
            Tokenizer source = new UAX29URLEmailTokenizer();
    
            // Filtramos las palabras vacías
            TokenStream result = new StopFilter(source, getStopWords());
    
            return new TokenStreamComponents(source, result);
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }
        };

      break;
      //SnowBallFilter
      case 4:
      filterName="SnowBallFilter";
      analyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
          try {
    
            // Tokenización
            Tokenizer source = new UAX29URLEmailTokenizer();
    
            // Stemmer
            TokenStream snowBall_filter = new SnowballFilter(source, "Spanish");
    
            return new TokenStreamComponents(source, snowBall_filter);
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }
        };

      break;
      //ShingleFilter
      case 5:   
      filterName="ShingleFilter";
      analyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
          try {
            // Tokenización
            Tokenizer source = new UAX29URLEmailTokenizer();
    
            // Filtramos con shingle
            TokenStream  shingle_filter = new ShingleFilter(source); // shingle_size = 2 por defecto
    
            return new TokenStreamComponents(source, shingle_filter);
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }
        };

      break;
      //EdgeNGramFilter
      case 6:
      filterName="EdgeNGramFilter";
      analyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
          try {  
            // Tokenización
            Tokenizer source = new UAX29URLEmailTokenizer();
    
            // Filtramos con EdgeNGramFilter
            int gramSize = 2; // tamaño del grano para la generación de bigramas
            TokenStream edge_filter = new EdgeNGramTokenFilter(source, gramSize, gramSize+1); // estamos en version 7.1 
          
            return new TokenStreamComponents(source, edge_filter);
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }
        };

      break;
      //NGramTokenFilter
      case 7:
      filterName="NGramTokenFilter";
      analyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
          try {
            // Tokenización
            Tokenizer source = new UAX29URLEmailTokenizer();
    
            // Filtramos con NGramTokenFilter
            int gramSize = 2; // tamaño del grano para la generación de bigramas
            TokenStream nGramToken_filter = new NGramTokenFilter(source, gramSize, gramSize+1);
    
            return new TokenStreamComponents(source, nGramToken_filter);
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }
        };

      break;
      //CommonGramsFilter
      case 8:
      filterName="CommonGramsFilter";
      analyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
          try {  
            // Tokenización
            Tokenizer source = new UAX29URLEmailTokenizer();
    
            // Filtramos con CommonGramsFilters
            TokenStream commonGram_filter =  new CommonGramsFilter(source, getCommonWords());
    
            return new TokenStreamComponents(source, commonGram_filter);
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }
        };

      break;
      //SyonymFilter
      case 9:
      filterName="SyonymFilter";
      analyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
          try {
            // Tokenización
            Tokenizer source = new UAX29URLEmailTokenizer();
    
            // Filtramos con SyonymFilter
            TokenStream result = new SynonymFilter(source, getSynonymMap(), false);
    
            return new TokenStreamComponents(source, result);
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }
        };
      break;
      default:
      analyzer = new StandardAnalyzer();
      break;
    }

    TokenStream stream = analyzer.tokenStream(null, new StringReader(contenido));
    CharTermAttribute cAtt = stream.getAttribute(CharTermAttribute.class);

    stream.reset();

    ArrayList<String> text = new ArrayList<String>();

    text.add("\n");
    text.add(filterName.toUpperCase());
    text.add("\n");

    while (stream.incrementToken()) {
      text.add(cAtt.toString());
    }
    stream.end();
    analyzer.close();

    return text;
  }

  public List<String> last4CaractersFilter() throws IOException{
    Analyzer analyzer;
    analyzer = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        try {
          // Tokenización
          Tokenizer source = new UAX29URLEmailTokenizer();

          // Filtramos con el filtro customizado (se queda con los 4 ultimos caracteres de cada token)
          TokenStream result = new CustomFilter(source);
  
          return new TokenStreamComponents(source, result);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
      };

    TokenStream stream = analyzer.tokenStream(null, new StringReader(contenido));
    CharTermAttribute cAtt = stream.getAttribute(CharTermAttribute.class);

    stream.reset();

    ArrayList<String> text = new ArrayList<String>();
    while (stream.incrementToken()) {
      text.add(cAtt.toString());
    }
    stream.end();
    analyzer.close();

    return text; 
  }


}