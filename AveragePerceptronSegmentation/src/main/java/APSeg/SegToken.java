package APSeg;

public class SegToken 
{
    public String word;
    public String type;

    public int startOffset;

    public int endOffset;


    public SegToken(String word, int startOffset, int endOffset) 
    {
        this.word = word;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.type = "unknown";
    }


    @Override
    public String toString() 
    {
        //return "[" + word + ", " + startOffset + ", " + endOffset + "]";
    	return word + "/" +  type + " ";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null){
            return false;
        }
        if (this == obj){
            return true;
        }
        if (obj instanceof SegToken){
            SegToken o = (SegToken) obj;
            return this.word == o.word && this.type == o.type;
        }
        return false;
    }
}
