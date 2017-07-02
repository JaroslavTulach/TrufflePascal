
package cz.cuni.mff.d3s.trupple.parser.wirth;

import cz.cuni.mff.d3s.trupple.parser.Token;
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.HashMap;


//-----------------------------------------------------------------------------------
// Buffer
//-----------------------------------------------------------------------------------
class Buffer {
	// This Buffer supports the following cases:
	// 1) seekable stream (file)
	//    a) whole stream in buffer
	//    b) part of stream in buffer
	// 2) non seekable stream (network, console)

	public static final int EOF = Character.MAX_VALUE + 1;
	private static final int MIN_BUFFER_LENGTH = 1024; // 1KB
	private static final int MAX_BUFFER_LENGTH = MIN_BUFFER_LENGTH * 64; // 64KB
	private byte[] buf;   // input buffer
	private int bufStart; // position of first byte in buffer relative to input stream
	private int bufLen;   // length of buffer
	private int fileLen;  // length of input stream (may change if stream is no file)
	private int bufPos;      // current position in buffer
	private RandomAccessFile file; // input stream (seekable)
	private InputStream stream; // growing input stream (e.g.: console, network)

	public Buffer(InputStream s) {
		stream = s;
		fileLen = bufLen = bufStart = bufPos = 0;
		buf = new byte[MIN_BUFFER_LENGTH];
	}

	public Buffer(String fileName) {
		try {
			file = new RandomAccessFile(fileName, "r");
			fileLen = (int) file.length();
			bufLen = Math.min(fileLen, MAX_BUFFER_LENGTH);
			buf = new byte[bufLen];
			bufStart = Integer.MAX_VALUE; // nothing in buffer so far
			if (fileLen > 0) setPos(0); // setup buffer to position 0 (start)
			else bufPos = 0; // index 0 is already after the file, thus setPos(0) is invalid
			if (bufLen == fileLen) Close();
		} catch (IOException e) {
			throw new FatalError("Could not open file " + fileName);
		}
	}

	// don't use b after this call anymore
	// called in UTF8Buffer constructor
	protected Buffer(Buffer b) {
		buf = b.buf;
		bufStart = b.bufStart;
		bufLen = b.bufLen;
		fileLen = b.fileLen;
		bufPos = b.bufPos;
		file = b.file;
		stream = b.stream;
		// keep finalize from closing the file
		b.file = null;
	}

	protected void finalize() throws Throwable {
		super.finalize();
		Close();
	}

	protected void Close() {
		if (file != null) {
			try {
				file.close();
				file = null;
			} catch (IOException e) {
				throw new FatalError(e.getMessage());
			}
		}
	}

	public int Read() {
		if (bufPos < bufLen) {
			return buf[bufPos++] & 0xff;  // mask out sign bits
		} else if (getPos() < fileLen) {
			setPos(getPos());         // shift buffer start to pos
			return buf[bufPos++] & 0xff; // mask out sign bits
		} else if (stream != null && ReadNextStreamChunk() > 0) {
			return buf[bufPos++] & 0xff;  // mask out sign bits
		} else {
			return EOF;
		}
	}

	public int Peek() {
		int curPos = getPos();
		int ch = Read();
		setPos(curPos);
		return ch;
	}

	// beg .. begin, zero-based, inclusive, in byte
	// end .. end, zero-based, exclusive, in byte
	public String GetString(int beg, int end) {
		int len = 0;
		char[] buf = new char[end - beg];
		int oldPos = getPos();
		setPos(beg);
		while (getPos() < end) buf[len++] = (char) Read();
		setPos(oldPos);
		return new String(buf, 0, len);
	}

	public int getPos() {
		return bufPos + bufStart;
	}

	public void setPos(int value) {
		if (value >= fileLen && stream != null) {
			// Wanted position is after buffer and the stream
			// is not seek-able e.g. network or console,
			// thus we have to read the stream manually till
			// the wanted position is in sight.
			while (value >= fileLen && ReadNextStreamChunk() > 0);
		}

		if (value < 0 || value > fileLen) {
			throw new FatalError("buffer out of bounds access, position: " + value);
		}

		if (value >= bufStart && value < bufStart + bufLen) { // already in buffer
			bufPos = value - bufStart;
		} else if (file != null) { // must be swapped in
			try {
				file.seek(value);
				bufLen = file.read(buf);
				bufStart = value; bufPos = 0;
			} catch(IOException e) {
				throw new FatalError(e.getMessage());
			}
		} else {
			// set the position to the end of the file, Pos will return fileLen.
			bufPos = fileLen - bufStart;
		}
	}
	
	// Read the next chunk of bytes from the stream, increases the buffer
	// if needed and updates the fields fileLen and bufLen.
	// Returns the number of bytes read.
	private int ReadNextStreamChunk() {
		int free = buf.length - bufLen;
		if (free == 0) {
			// in the case of a growing input stream
			// we can neither seek in the stream, nor can we
			// foresee the maximum length, thus we must adapt
			// the buffer size on demand.
			byte[] newBuf = new byte[bufLen * 2];
			System.arraycopy(buf, 0, newBuf, 0, bufLen);
			buf = newBuf;
			free = bufLen;
		}
		
		int read;
		try { read = stream.read(buf, bufLen, free); }
		catch (IOException ioex) { throw new FatalError(ioex.getMessage()); }
		
		if (read > 0) {
			fileLen = bufLen = (bufLen + read);
			return read;
		}
		// end of stream reached
		return 0;
	}
}

//-----------------------------------------------------------------------------------
// UTF8Buffer
//-----------------------------------------------------------------------------------
class UTF8Buffer extends Buffer {
	UTF8Buffer(Buffer b) { super(b); }

	public int Read() {
		int ch;
		do {
			ch = super.Read();
			// until we find a utf8 start (0xxxxxxx or 11xxxxxx)
		} while ((ch >= 128) && ((ch & 0xC0) != 0xC0) && (ch != EOF));
		if (ch < 128 || ch == EOF) {
			// nothing to do, first 127 chars are the same in ascii and utf8
			// 0xxxxxxx or end of file character
		} else if ((ch & 0xF0) == 0xF0) {
			// 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
			int c1 = ch & 0x07; ch = super.Read();
			int c2 = ch & 0x3F; ch = super.Read();
			int c3 = ch & 0x3F; ch = super.Read();
			int c4 = ch & 0x3F;
			ch = (((((c1 << 6) | c2) << 6) | c3) << 6) | c4;
		} else if ((ch & 0xE0) == 0xE0) {
			// 1110xxxx 10xxxxxx 10xxxxxx
			int c1 = ch & 0x0F; ch = super.Read();
			int c2 = ch & 0x3F; ch = super.Read();
			int c3 = ch & 0x3F;
			ch = (((c1 << 6) | c2) << 6) | c3;
		} else if ((ch & 0xC0) == 0xC0) {
			// 110xxxxx 10xxxxxx
			int c1 = ch & 0x1F; ch = super.Read();
			int c2 = ch & 0x3F;
			ch = (c1 << 6) | c2;
		}
		return ch;
	}
}

//-----------------------------------------------------------------------------------
// StartStates  -- maps characters to start states of tokens
//-----------------------------------------------------------------------------------
class StartStates {
	private static class Elem {
		public int key, val;
		public Elem next;
		public Elem(int key, int val) { this.key = key; this.val = val; }
	}

	private Elem[] tab = new Elem[128];

	public void set(int key, int val) {
		Elem e = new Elem(key, val);
		int k = key % 128;
		e.next = tab[k]; tab[k] = e;
	}

	public int state(int key) {
		Elem e = tab[key % 128];
		while (e != null && e.key != key) e = e.next;
		return e == null ? 0: e.val;
	}
}

//-----------------------------------------------------------------------------------
// Scanner
//-----------------------------------------------------------------------------------
public class Scanner {
	static final char EOL = '\n';
	static final int  eofSym = 0;
	static final int maxT = 63;
	static final int noSym = 63;
	char valCh;       // current input character (for token.val)

	public Buffer buffer; // scanner buffer

	Token t;           // current token
	int ch;            // current input character
	int pos;           // byte position of current character
	int charPos;       // position by unicode characters starting with 0
	int col;           // column number of current character
	int line;          // line number of current character
	int oldEols;       // EOLs that appeared in a comment;
	static final StartStates start; // maps initial token character to start state
	static final Map<String, Integer> literals;      // maps literal strings to literal kinds

	Token tokens;      // list of tokens already peeked (first token is a dummy)
	Token pt;          // current peek token
	
	char[] tval = new char[16]; // token text used in NextToken(), dynamically enlarged
	int tlen;          // length of current token


	static {
		start = new StartStates();
		literals = new HashMap<String, Integer>();
		for (int i = 97; i <= 122; ++i) start.set(i, 1);
		for (int i = 49; i <= 57; ++i) start.set(i, 9);
		start.set(39, 2); 
		start.set(48, 10); 
		start.set(40, 12); 
		start.set(41, 13); 
		start.set(59, 14); 
		start.set(44, 15); 
		start.set(61, 16); 
		start.set(91, 17); 
		start.set(93, 18); 
		start.set(94, 19); 
		start.set(58, 29); 
		start.set(46, 30); 
		start.set(43, 21); 
		start.set(45, 22); 
		start.set(62, 31); 
		start.set(60, 32); 
		start.set(42, 27); 
		start.set(47, 28); 
		start.set(Buffer.EOF, -1);
		literals.put("program", new Integer(5));
		literals.put("label", new Integer(9));
		literals.put("type", new Integer(11));
		literals.put("of", new Integer(13));
		literals.put("set", new Integer(14));
		literals.put("packed", new Integer(15));
		literals.put("array", new Integer(16));
		literals.put("file", new Integer(19));
		literals.put("record", new Integer(20));
		literals.put("end", new Integer(21));
		literals.put("case", new Integer(23));
		literals.put("const", new Integer(26));
		literals.put("var", new Integer(29));
		literals.put("forward", new Integer(30));
		literals.put("function", new Integer(31));
		literals.put("procedure", new Integer(32));
		literals.put("begin", new Integer(34));
		literals.put("else", new Integer(36));
		literals.put("with", new Integer(37));
		literals.put("do", new Integer(38));
		literals.put("for", new Integer(39));
		literals.put("to", new Integer(40));
		literals.put("downto", new Integer(41));
		literals.put("repeat", new Integer(42));
		literals.put("until", new Integer(43));
		literals.put("while", new Integer(44));
		literals.put("if", new Integer(45));
		literals.put("then", new Integer(46));
		literals.put("goto", new Integer(47));
		literals.put("or", new Integer(48));
		literals.put("and", new Integer(49));
		literals.put("not", new Integer(50));
		literals.put("in", new Integer(56));
		literals.put("div", new Integer(59));
		literals.put("mod", new Integer(60));
		literals.put("true", new Integer(61));
		literals.put("false", new Integer(62));

	}
	
	public Scanner (String fileName) {
		buffer = new Buffer(fileName);
		Init();
	}
	
	public Scanner(InputStream s) {
		buffer = new Buffer(s);
		Init();
	}
	
	void Init () {
		pos = -1; line = 1; col = 0; charPos = -1;
		oldEols = 0;
		NextCh();
		if (ch == 0xEF) { // check optional byte order mark for UTF-8
			NextCh(); int ch1 = ch;
			NextCh(); int ch2 = ch;
			if (ch1 != 0xBB || ch2 != 0xBF) {
				throw new FatalError("Illegal byte order mark at start of file");
			}
			buffer = new UTF8Buffer(buffer); col = 0; charPos = -1;
			NextCh();
		}
		pt = tokens = new Token();  // first token is a dummy
	}
	
	void NextCh() {
		if (oldEols > 0) { ch = EOL; oldEols--; }
		else {
			pos = buffer.getPos();
			// buffer reads unicode chars, if UTF8 has been detected
			ch = buffer.Read(); col++; charPos++;
			// replace isolated '\r' by '\n' in order to make
			// eol handling uniform across Windows, Unix and Mac
			if (ch == '\r' && buffer.Peek() != '\n') ch = EOL;
			if (ch == EOL) { line++; col = 0; }
		}
		if (ch != Buffer.EOF) {
			valCh = (char) ch;
			ch = Character.toLowerCase(ch);
		}

	}
	
	void AddCh() {
		if (tlen >= tval.length) {
			char[] newBuf = new char[2 * tval.length];
			System.arraycopy(tval, 0, newBuf, 0, tval.length);
			tval = newBuf;
		}
		if (ch != Buffer.EOF) {
			tval[tlen++] = valCh; 

			NextCh();
		}

	}
	
	@SuppressWarnings("unused")

	boolean Comment0() {
		int level = 1, pos0 = pos, line0 = line, col0 = col, charPos0 = charPos;
		NextCh();
			for(;;) {
				if (ch == '}') {
					level--;
					if (level == 0) { oldEols = line - line0; NextCh(); return true; }
					NextCh();
				} else if (ch == Buffer.EOF) return false;
				else NextCh();
			}
	}

	boolean Comment1() {
		int level = 1, pos0 = pos, line0 = line, col0 = col, charPos0 = charPos;
		NextCh();
		if (ch == '*') {
			NextCh();
			for(;;) {
				if (ch == '*') {
					NextCh();
					if (ch == ')') {
						level--;
						if (level == 0) { oldEols = line - line0; NextCh(); return true; }
						NextCh();
					}
				} else if (ch == Buffer.EOF) return false;
				else NextCh();
			}
		} else {
			buffer.setPos(pos0); NextCh(); line = line0; col = col0; charPos = charPos0;
		}
		return false;
	}


	void CheckLiteral() {
		String val = t.val;
		val = val.toLowerCase();

		Object kind = literals.get(val);
		if (kind != null) {
			t.kind = ((Integer) kind).intValue();
		}
	}

	Token NextToken() {
		while (ch == ' ' ||
			ch >= 9 && ch <= 10 || ch == 13
		) NextCh();
		if (ch == '{' && Comment0() ||ch == '(' && Comment1()) return NextToken();
		int recKind = noSym;
		int recEnd = pos;
		t = new Token();
		t.pos = pos; t.col = col; t.line = line; t.charPos = charPos;
		int state = start.state(ch);
		tlen = 0; AddCh();

		loop: for (;;) {
			switch (state) {
				case -1: { t.kind = eofSym; break loop; } // NextCh already done 
				case 0: {
					if (recKind != noSym) {
						tlen = recEnd - t.pos;
						SetScannerBehindT();
					}
					t.kind = recKind; break loop;
				} // NextCh already done
				case 1:
					recEnd = pos; recKind = 1;
					if (ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'z') {AddCh(); state = 1; break;}
					else {t.kind = 1; t.val = new String(tval, 0, tlen); CheckLiteral(); return t;}
				case 2:
					if (ch <= 9 || ch >= 11 && ch <= 12 || ch >= 14 && ch <= '&' || ch >= '(' && ch <= 65535) {AddCh(); state = 2; break;}
					else if (ch == 39) {AddCh(); state = 11; break;}
					else {state = 0; break;}
				case 3:
					recEnd = pos; recKind = 4;
					if (ch == '+' || ch == '-' || ch >= '0' && ch <= '9') {AddCh(); state = 4; break;}
					else {t.kind = 4; break loop;}
				case 4:
					recEnd = pos; recKind = 4;
					if (ch >= '0' && ch <= '9') {AddCh(); state = 4; break;}
					else {t.kind = 4; break loop;}
				case 5:
					if (ch >= '0' && ch <= '9') {AddCh(); state = 6; break;}
					else {state = 0; break;}
				case 6:
					recEnd = pos; recKind = 4;
					if (ch >= '0' && ch <= '9') {AddCh(); state = 6; break;}
					else if (ch == 'e') {AddCh(); state = 7; break;}
					else {t.kind = 4; break loop;}
				case 7:
					recEnd = pos; recKind = 4;
					if (ch == '+' || ch == '-' || ch >= '0' && ch <= '9') {AddCh(); state = 8; break;}
					else {t.kind = 4; break loop;}
				case 8:
					recEnd = pos; recKind = 4;
					if (ch >= '0' && ch <= '9') {AddCh(); state = 8; break;}
					else {t.kind = 4; break loop;}
				case 9:
					recEnd = pos; recKind = 3;
					if (ch >= '0' && ch <= '9') {AddCh(); state = 9; break;}
					else if (ch == 'e') {AddCh(); state = 3; break;}
					else if (ch == '.') {AddCh(); state = 5; break;}
					else {t.kind = 3; break loop;}
				case 10:
					recEnd = pos; recKind = 3;
					if (ch == 'e') {AddCh(); state = 3; break;}
					else if (ch == '.') {AddCh(); state = 5; break;}
					else {t.kind = 3; break loop;}
				case 11:
					recEnd = pos; recKind = 2;
					if (ch == 39) {AddCh(); state = 2; break;}
					else {t.kind = 2; break loop;}
				case 12:
					{t.kind = 6; break loop;}
				case 13:
					{t.kind = 7; break loop;}
				case 14:
					{t.kind = 8; break loop;}
				case 15:
					{t.kind = 10; break loop;}
				case 16:
					{t.kind = 12; break loop;}
				case 17:
					{t.kind = 17; break loop;}
				case 18:
					{t.kind = 18; break loop;}
				case 19:
					{t.kind = 22; break loop;}
				case 20:
					{t.kind = 25; break loop;}
				case 21:
					{t.kind = 27; break loop;}
				case 22:
					{t.kind = 28; break loop;}
				case 23:
					{t.kind = 35; break loop;}
				case 24:
					{t.kind = 52; break loop;}
				case 25:
					{t.kind = 54; break loop;}
				case 26:
					{t.kind = 55; break loop;}
				case 27:
					{t.kind = 57; break loop;}
				case 28:
					{t.kind = 58; break loop;}
				case 29:
					recEnd = pos; recKind = 24;
					if (ch == '=') {AddCh(); state = 23; break;}
					else {t.kind = 24; break loop;}
				case 30:
					recEnd = pos; recKind = 33;
					if (ch == '.') {AddCh(); state = 20; break;}
					else {t.kind = 33; break loop;}
				case 31:
					recEnd = pos; recKind = 51;
					if (ch == '=') {AddCh(); state = 24; break;}
					else {t.kind = 51; break loop;}
				case 32:
					recEnd = pos; recKind = 53;
					if (ch == '=') {AddCh(); state = 25; break;}
					else if (ch == '>') {AddCh(); state = 26; break;}
					else {t.kind = 53; break loop;}

			}
		}
		t.val = new String(tval, 0, tlen);
		return t;
	}
	
	private void SetScannerBehindT() {
		buffer.setPos(t.pos);
		NextCh();
		line = t.line; col = t.col; charPos = t.charPos;
		for (int i = 0; i < tlen; i++) NextCh();
	}
	
	// get the next token (possibly a token already seen during peeking)
	public Token Scan () {
		if (tokens.next == null) {
			return NextToken();
		} else {
			pt = tokens = tokens.next;
			return tokens;
		}
	}

	// get the next token, ignore pragmas
	public Token Peek () {
		do {
			if (pt.next == null) {
				pt.next = NextToken();
			}
			pt = pt.next;
		} while (pt.kind > maxT); // skip pragmas

		return pt;
	}

	// make sure that peeking starts at current scan position
	public void ResetPeek () { pt = tokens; }

} // end Scanner
