/*���������ʽ�Ĵʷ��������������ַ����ҷ�����Ӧ��ǩ*/
public class Lexer {

	//Enumһ��������ʾһ����ͬ���͵ĳ���
	public enum Token{
	EOS,  //������ʽĩβ����ʾ�ѷ�����ϡ�
	ANY, //.ͨ���
	AT_BOL,   // ^��ͷƥ���
	AT_EOL,  // $ĩβƥ���
   CCL_START, //�ַ�����ʼ����[
	CCL_END, // �ַ������β���� ]
	CLOSE_CURLY, //   }
	CLOSE_PAREN,//    )
	CLOSURE,    //   *
	DASH, //   -
	END_OF_INPUT, //�����������
	L,    //�ַ�����
	
	OPEN_CURLY,  //{
	OPEN_PAREN,  //(
	OPTIONAL,   // ?
	OR, //  |
	PLUS_CLOSE
			
	};
	
	
	private final int ASCII_COUNT=128;//һ��ö�����͵����飿������128��
	private Token[] tokenMap=new Token[ASCII_COUNT];
	private Token currentToken = Token.EOS; //��ֵ

	RegularExpressionHandler exprHandler=null;
	
	private int exprCount = 0;
	private String curExpr = "";//�ַ���
	private int charIndex = 0;//��ʾ�����ַ�����������ʽ�ַ����е��±�
	private  boolean inQuoted = false;//�Ƿ���˫������
	private boolean sawEsc =false; //�Ƿ��ȡ��ת�Ʒ�/
	private int lexeme;
	
	//���췽��
	public Lexer(RegularExpressionHandler exprHandler) {
		initTokenMap();
		this.exprHandler = exprHandler; //���︳ֵ
		
	}	
		
	private void initTokenMap() {
		for(int i =0;i<ASCII_COUNT;i++) {
             tokenMap[i]=Token.L;
      //�ڿ�ʼ��ʱ��Ĭ��ÿһ���ַ�ÿһ����ǩ�����ַ�����L		
		
		}
	
	     //��������Ҷ����ַ���Ӧ�ı�ǩ��tokenmap�����Ȼ�󸳳�����
		    tokenMap['.'] = Token.ANY;//����ǵ㣬��ֵΪToken.ANY
	        tokenMap['^'] = Token.AT_BOL;
	        tokenMap['$'] = Token.AT_EOL;
	        tokenMap[']'] = Token.CCL_END;
	        tokenMap['['] = Token.CCL_START;
	        tokenMap['}'] = Token.CLOSE_CURLY;
	        tokenMap[')'] = Token.CLOSE_PAREN;
	        tokenMap['*'] = Token.CLOSURE;
	        tokenMap['-'] = Token.DASH;
	        tokenMap['{'] = Token.OPEN_CURLY;
	        tokenMap['('] = Token.OPEN_PAREN;
	        tokenMap['?'] = Token.OPTIONAL;
	        tokenMap['|'] = Token.OR;
	        tokenMap['+'] = Token.PLUS_CLOSE; 
		
	}
	 
	
 /*advance�ӿ����������ַ����ҷ��ض�Ӧ�ı�ǩ��
  �ڽӿ���������ͨ��exprHander����Ѿ�����õ�������ʽ�ַ��������δ��Ի�ȡ�õ���ȡ����
  ���������
  * 
  * 
	*/
	public Token advance() {
//����ΪEOS,��ʼ��ȡ������ʽ����([0-9]+)�����exprHandler�õ��ı��궨�崦��õı��ʽ
	if(currentToken ==Token.EOS) {
	//2 �����ǰ��ǩ��EOS,һ��������ʽ���������������һ�����ʽ
    if (exprCount >= exprHandler.getRegularExpressionCount()) {
	//����������ʽ������� 
	currentToken=Token.END_OF_INPUT;//END_OF_INPUT �����������
	        return currentToken;
        }else {
        	curExpr = exprHandler.getRegularExpression(exprCount);//��ֵ
			exprCount++;
        }
	}
	// 1 ���ַ��±�>�ַ������ʽ����ʱ����ʾ��ǰ�ַ����������
	if (charIndex >= curExpr.length()) {
		currentToken = Token.EOS;
		charIndex = 0;
		return currentToken; 
	}
		
//����˫��������� �� charAt��) �����ɷ���ָ��λ�õ��ַ�,��һ���ַ�λ��Ϊ 0, �ڶ����ַ�λ��Ϊ 1,�Դ�����.�确abc���ַ���,a���±���0��b1��c3
	
	if (curExpr.charAt(charIndex) == '"') {
		inQuoted = !inQuoted; //false-true-flase ѭ������
		charIndex++;
	}
	


	sawEsc = (curExpr.charAt(charIndex) == '\\'); //��ǰ�������ַ������±���������һ��(��һ���ǲ���'���ţ����Ǿͽ���������һ��
	if (sawEsc && curExpr.charAt(charIndex + 1) != '"' && inQuoted == false) {
		lexeme = handleEsc();
	}
	else {
		if (sawEsc && curExpr.charAt(charIndex + 1) == '"') {
			charIndex += 2;
			lexeme = '"';
		}
		else {
			lexeme = curExpr.charAt(charIndex);
			charIndex++;//�õ���Ӧ��asciiֵ
		}
	}
	
	//��ǰ�����ַ��Ƿ���˫�����ڻ�ǰ����ת�����������������ͨ�ַ�����(����L)����Ȼ����tokenMapѰ�����Ӧ�ı�ǩֵ
	currentToken = (inQuoted || sawEsc) ? Token.L : tokenMap[lexeme]; //lexeme�洢���ǵ�ǰ�ַ���ֵ
	
	return currentToken;

	}
  //Ȼ������ThompsonConstruction �ķ����У���pritlnResult��ӡ���/

	
	
  //������б�����⴦����ת���ַ����״���ĺ�����handleEsc
	
	private int handleEsc() {
    	/*��ת�Ʒ� \ ����ʱ���������������������ַ����ַ���һ����
    	 *���Ǵ����ת���ַ������¼�����ʽ
    	 * \b backspace ɾ��
    	 * \f formfeed
    	 * \n newline ����
    	 * \r carriage return �س�
    	 * \s space �ո�
    	 * \t tab
    	 * \e ASCII ESC ('\033')
    	 * \DDD 3λ�˽�����
    	 * \xDDD 3λʮ��������
    	 * \^C C���κ��ַ��� ����^A, ^B ��Ascii ���ж��ж�Ӧ�����⺬��
    	 * ASCII �ַ���μ���
    	 * http://baike.baidu.com/pic/%E7%BE%8E%E5%9B%BD%E4%BF%A1%E6%81%AF%E4%BA%A4%E6%8D%A2%E6%A0%87%E5%87%86%E4%BB%A3%E7%A0%81/8950990/0/9213b07eca8065387d4c671896dda144ad348213?fr=lemma&ct=single#aid=0&pic=9213b07eca8065387d4c671896dda144ad348213
    	 */
	
  int rval = 0;
  String exprToUpper=curExpr.toUpperCase();//toUpperCase() �������ڽ�Сд�ַ�ת��Ϊ��д��
  charIndex++; //Խ��ת�Ʒ� \	
  //�ַ��������±��ȡ��,ת��Ϊ��д���ַ�������Ϊswitch�ı��ʽ
  switch (exprToUpper.charAt(charIndex)) {
  case '\0' : 
	  rval = '\\'; //ÿ��case��ʾ��ͬ�����
	  break;
case 'B': 
	  rval = '\b';//������B����'\b'��asciiֵ��ֵ��rval
	  break;
case 'F':
	  rval = '\f';
	  break;
case 'N' :
	  rval = '\n';  // \n��Ӧ��asciiֵΪ10 ��ֵ��N
	  break;
case 'R' :
	  rval = '\r';
	  break;
case 'S':
	  rval = ' ';
	  break;
case 'T':
	  rval = '\t';
	  break;
case 'E' :
	  rval = '\033';
	  break;
case '^':
	  charIndex++;
	  /*
	   * ��˵�����^�������һ����ĸʱ����ʾ������ǿ����ַ�
	   * ^@ ��ASCII ���е���ֵΪ0��^A Ϊ1, �ַ�@��ASCII ������ֵΪ80�� �ַ�A��ASCII������ֵΪ81
	   * 'A' - '@' ����1 �Ͷ�Ӧ ^A �� ASCII ���е�λ��
	   * ����ɲο�ע�͸�����ASCII ͼ
	   * 
	   */
	  rval = (char) (curExpr.charAt(charIndex) - '@');
	  break;
case 'X':
	/*
	 * \X ��ʾ������ŵ������ַ���ʾ�˽��ƻ�ʮ��������
	 */
	charIndex++; //Խ��X
	if (isHexDigit(curExpr.charAt(charIndex))) {
		rval = hex2Bin(curExpr.charAt(charIndex));
		charIndex++;
	}
	
	if (isHexDigit(curExpr.charAt(charIndex))) {
		rval <<= 4;
		rval |= hex2Bin(curExpr.charAt(charIndex));
		charIndex++;
	}
	
	if (isHexDigit(curExpr.charAt(charIndex))) {
		rval <<= 4;
		rval |= hex2Bin(curExpr.charAt(charIndex));
		charIndex++;
	}
	charIndex--; //�����ں����ײ����charIndex++ ���������� --
	break;
	
	default:
		if (isOctDigit(curExpr.charAt(charIndex)) == false) {
			rval = curExpr.charAt(charIndex);
		}
		else {
			charIndex++;
			rval = oct2Bin(curExpr.charAt(charIndex));
			charIndex++;
			if (isOctDigit(curExpr.charAt(charIndex))) {
				rval <<= 3;
				rval |= oct2Bin(curExpr.charAt(charIndex));
				charIndex++;
			}
			
			if (isOctDigit(curExpr.charAt(charIndex))) {
				rval <<= 3;
				rval |= oct2Bin(curExpr.charAt(charIndex));
				charIndex++;
			}
			
			charIndex--;//�����ں����ײ����charIndex++ ���������� --
		}		
}

charIndex++;
return rval;
}
	  private int hex2Bin(char c) {
	    	/*
	    	 * ��ʮ����������Ӧ���ַ�ת��Ϊ��Ӧ����ֵ������
	    	 * A ת��Ϊ10�� Bת��Ϊ11
	    	 * �ַ�c ��������ʮ�������ַ��� 0123456789ABCDEF
	    	 */
	    	return (Character.isDigit(c) ? (c) - '0' : (Character.toUpperCase(c) - 'A' + 10)) & 0xf;
	    }
	    
	    private int oct2Bin(char c) {
	    	/*
	    	 * ���ַ�c ת��Ϊ��Ӧ�İ˽�����
	    	 * �ַ�c �����ǺϷ��İ˽����ַ�: 01234567
	    	 */
	    	return ((c) - '0') & 0x7;
	    }
	    
	    private boolean isHexDigit(char c) {	
	    	return (Character.isDigit(c)|| ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F'));
	    }
	    
	    private boolean isOctDigit(char c) {
	    	return ('0' <= c && c <= '7');
	    }
	
  
  
  
  
  
		
		
	}
	
	
	
	
	
	
	
	
	
	
}
