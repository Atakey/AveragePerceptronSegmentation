package APSeg;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by Huanghl on 2019/4/9.
 */
public class SegUtils
{
    static final char BeginEnd = '#';
    static final char punctuation = '。';
    static final char digit = '1';
    static final char letter = 'a';
//    static final char name = 'n';
//    static final char place = 'p';
      //叠词特征
//    public static final String ABAB = "A";
//    public static final String AABB = "B";
//    public static final String ABCD = "C";
    //未登录词特征
    static final String UNKNOWN = "u";
    //备注：以上几个字母赋值不要重复即可

    static Map<Integer, Character> map = new FeatureMap<>();
    static
    {
        map.put(0, BeginEnd);
        map.put(1, punctuation);//punctuation
        map.put(2, digit);//digit
        map.put(3, letter);//letter
        //do something else
        //不建议对正常中文进行替换
//        map.put(4, name);
//        map.put(5, place);
//        map.put(6, job);
    }

    static Map<Character, Integer> totalMap = new FeatureMap<>();
    static
    {
        for(char punc:new char[]{' ', '!', '"', '#', '$', '%', '&', '\'',
                '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<',
                '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{',
                '|', '}', '~', '°', '·', '×', 'Φ', '–', '—', '―', '‘',
                '’', '“', '”', '…', '‰', '′', '※', '℃', '℉', '→', '↓',
                '∥', '∶', '≈', '≠', '≥', '▂', '█', '▌', '■', '□', '▲',
                '△', '▼', '◆', '◇', '○', '◎', '●', '★', '☆', '、', '。',
                '〈', '〉', '《', '》', '「', '」', '『', '』', '【', '】',
                '〔', '〕', '㎡', '！', '＂', '＃', '＄', '％', '＆', '＇',
                '（', '）', '＊', '＋', '，', '－', '．', '／', '：', '；',
                '＜', '＝', '＞', '？', '＠', '［', '＼', '］', '＾', '＿','↘',
                '｀', '｛', '｜', '｝', '～', '￥','\u007F','§','÷','Δ','″',
                '∈', '∠', '∞', '∽', '≡', '≮', '∕', '√', '∵','∴', '⊙', '≤',
                '⊥', '─', '│', '╱', '〖','¨','ˉ','Λ','Ω','←','↑','∫',
                '∧','∨','∩','≌','⌒','┃'
        })
            totalMap.put(punc, 1);

        totalMap.put(BeginEnd, 0);

        for(char digit:("0123456789" +
                "ΙΧⅠⅡⅢⅣⅤⅥⅧⅨⅰⅱ①②③④⑤⑥⑦⑧⑨⑩⑵⑶⑹⒃⒅" +
                "〇一二三四五六七八九十亿百千万零壹貳贰叁肆伍陆柒捌玖拾佰仟萬陸" +
                "０１２３４５６７８９" +
                //NOTE：可额外添加在下面
                "㎎㎜㎏"
                ).toCharArray())
            totalMap.put(digit, 2);

        for(char letter:("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
                "±àαβγδζμωＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ" +
                "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ" +
                "ōòǒóüūúǔùāáǎēéěèīíǐìńηЫΣεθλφπτυρвиклно" +
                "пцяеьКсбσгしのアイドマルレΓψぉんダんテㄇ♀♂" +
                //NOTE：可额外添加在下面
                ""
                ).toCharArray())
            totalMap.put(letter, 3);

        for(char name:("李王张刘陈杨赵黄周吴徐孙胡朱高林何郭马赖梁宋郑" +
                "谢韩唐冯于董萧程曹袁邓许傅沈曾彭吕苏卢蒋蔡贾丁魏薛叶阎" +
                "余潘杜戴夏钟汪田任姜范方石姚谭廖邹熊金陆郝孔白崔康毛邱" +
                "秦江史顾侯邵孟龙万段章钱汤尹黎易常武乔贺阮龚文" +
                //NOTE：可额外添加在下面
                ""
                ).toCharArray())
            totalMap.put(name, 4);

        for(char place:("坝邦堡城池村单岛道堤店洞渡队峰府冈港阁宫沟国海" +
                "号河湖环集江礁角街井郡坑口矿里岭楼路门盟庙弄牌派坡铺旗" +
                "桥区渠泉山省市水寺塔台滩坛堂厅亭屯湾屋溪峡县线乡巷洋窑" +
                "营屿园苑院闸寨站镇州庄族陂庵町" +
                //NOTE：可额外添加在下面
                ""
                ).toCharArray())
            totalMap.put(place, 5);
        for(char job:"董总老小".toCharArray())
        {
            totalMap.put(job, 6);
        }
//        do something else
    }

    /**
     * 获取字符类型，数字英文字母等,并做转换处理
     */
    static int GetWordType(char word)
    {
        Integer integer = totalMap.get(word);
        return null == integer ? 9 : integer;
    }

    static char GetWord(char word, int type)
    {
        Character _word = map.get(type);
        return null == _word ? word : _word;
    }

    static String FormatKey(String key, int len)
    {
        if(key.length() <= len)
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len - key.length(); i++)
                sb.append('0');
            sb.append(key);
            return sb.toString();
        }
        throw new IllegalArgumentException("the length of input key must be less than len...");
    }

    static float FormatValue(float value, int size)
    {
        BigDecimal b = new BigDecimal(value);
        return  b.setScale(size, BigDecimal.ROUND_HALF_UP).floatValue();
    }

    static double FormatValue(double value, int size)
    {
        BigDecimal b = new BigDecimal(value);
        return  b.setScale(size, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 用于筛选特征用
     * @param size 特征数量
     * @return
     */
    static ArrayList<boolean[]> Generate(int size)
    {
        int oSize = size;
        ArrayList<boolean[]> booleanList = new ArrayList<>();
        for (int i = (int)Math.pow(2, oSize)-1; i > 0; --i)
        {
            String s = FormatKey(Integer.toBinaryString(i), oSize);
//            if(s.replace("0","").length() > 0)
            {
                boolean[] booleans = new boolean[size];
                for (int j = 0; j < oSize; j++)
                    booleans[j] = s.charAt(j) == '0' ? false : true;
                booleanList.add(booleans);
            }
        }
        return booleanList;
    }
}