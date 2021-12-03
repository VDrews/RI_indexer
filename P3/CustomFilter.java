package P3;

import java.io.IOException;

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class CustomFilter extends FilteringTokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

   //Constructor
    public CustomFilter(TokenStream in) {
      super(in);
    }

    @Override
    protected boolean accept() throws IOException{
        String token = new String (termAtt.buffer(),0, termAtt.length());

        if(token.length()<=4){
            return false;
        }else{
            //Reescribimos el token con la cadena compuesta por los ultimos 4 caracteres.
            termAtt.copyBuffer(token.toCharArray(), token.length()-4, 4); 
            return true;
        }

    }

    
}
