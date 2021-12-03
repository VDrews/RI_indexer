package P3;

import org.apache.lucene.analysis.core.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.mchange.net.SocketUtils;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import org.apache.lucene.facet.*;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.document.Field;

public class IndiceSimple {

    String indexpath = "./index";
    String docPath = "./datasets";
    boolean create = true;
    private IndexWriter writer;
    private DirectoryTaxonomyWriter facet_writer;

    public static void main(String[] args) throws IOException, CsvException {

        Analyzer analyzer = new StandardAnalyzer();
        Similarity similarity = new ClassicSimilarity();
        IndiceSimple baseline = new IndiceSimple();
        IndiceSimple facet_index = new IndiceSimple();
        baseline.configurarIndice(analyzer, similarity);
        FacetsConfig fconfig = baseline.configurarIndice();

        File[] files;
        File directory = new File(args[0]);
        files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });

        for (File file : files) {
            baseline.indexarDocumentos(file, fconfig);
        }

        baseline.close();
    }

    // Método para configurar el indice.
    public void configurarIndice(Analyzer analyzer, Similarity similarity) throws IOException {
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setSimilarity(similarity);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        Directory dir = FSDirectory.open(Paths.get("./P3/index"));

        writer = new IndexWriter(dir, iwc);
    }

    public static String leerDocumento(File f) {
        return "";
    }

    // Método para configurar el indice de las facetas
    public FacetsConfig configurarIndice() throws IOException {
        FacetsConfig fconfig = new FacetsConfig();
        Directory dir = FSDirectory.open(Paths.get("./P3/facets"));

        facet_writer = new DirectoryTaxonomyWriter(dir);
        fconfig.setMultiValued("Author", true);
        fconfig.setMultiValued("Keyword", true);
        fconfig.setMultiValued("Year", true);
        return fconfig;
    }

    /*
     * public static List<String[]> leerCsv(Reader reader) throws IOException,
     * CsvException { CSVReader csvReader = new
     * CSVReaderBuilder(reader).withSkipLines(1).build(); List<String[]> list = new
     * ArrayList<>(); list = csvReader.readAll(); reader.close(); csvReader.close();
     * return list; }
     */

    // Método para recoger la informacion de indexacion de los documentos, y
    // añadirlos al indice.
    public void indexarDocumentos(File file, FacetsConfig fConfig)
            throws FileNotFoundException, IOException, CsvException {
        CSVReader reader = new CSVReader(new FileReader(file.getAbsoluteFile()));
        String subdoc[];
        reader.readNext(); // leemos la linea de headers sin recogerla.
        while ((subdoc = reader.readNext()) != null) {
            // new FileReader(fichero)
            Document doc = new Document();

            // System.out.println(subdoc[HEADERS.AuthorsID]);
            // System.out.println(subdoc[HEADERS.Title]);
            // System.out.println(subdoc[HEADERS.Year]);
            // System.out.println(subdoc[HEADERS.Abstract]);
            // System.out.println(subdoc[HEADERS.AuthorKeywords]);

            // INCLUIMOS LOS CAMPOS DE INDEXACIÓN

            final String[] authors = subdoc[HEADERS.Author].split(", ");
            final String[] keywords = subdoc[HEADERS.AuthorKeywords].split("; ");

            for (String author : authors) {
                System.out.println(author);
                doc.add(new StoredField("Author", author));
            }

            doc.add(new TextField("Title", subdoc[HEADERS.Title], Field.Store.YES));

            for (String keyword : keywords) {
                doc.add(new StoredField("Keyword", keyword));

            }

            doc.add(new TextField("Content", subdoc[HEADERS.Abstract], Field.Store.YES));
            doc.add(new StringField("EID", subdoc[HEADERS.EID], Field.Store.YES))

            // INCLUIMOS LOS CAMPOS DE INDEXACION DE LAS FACETAS

            ;
            for (String keyword : keywords) {
                if (keyword.length() > 0)
                    doc.add(new FacetField("Keyword", keyword));

            }
            doc.add(new FacetField("Year", subdoc[HEADERS.Year]));

            // System.out.println(facet_writer);
            writer.addDocument(fConfig.build(facet_writer, doc));

        }
    }

    // Método que maneja el cierre del indice.
    public void close() {
        try {
            writer.commit();
            writer.close();
            facet_writer.commit();
            facet_writer.close();
        } catch (IOException e) {
            System.out.println("¡Error cerrando el indice principal o el indice de las facetas!");
        }

    }

}
