package APSeg;


/**
 * 用户字典依赖的数据结构trie树
 */
public class UserDictTrieTree
{
	/**
	 * 内部节点类
	 */
	public class TrieNode
    {
        private boolean end = false;
		private FeatureMap<Character, TrieNode> trieMap;

		public TrieNode()
        {
            this.trieMap = new FeatureMap<>();
        }

		public boolean HasWord()
		{
			return trieMap.size() > 0;
		}

		public TrieNode getTrieNode(Character word)
		{
			return trieMap.get(word);
		}

		public boolean isEnd(){return end;}
	}

	TrieNode root; //树根

	public UserDictTrieTree()
	{
		this.root  = new TrieNode();
	}

	/**
	 * 插入字串，用循环代替迭代实现
	 * @param root
	 * @param word
	 */
	private void Insert(TrieNode root, String word)
	{
		//只支持词长大于1才有意义
		if(null == word || word.length() < 2) return;

		for(int i = 0; i < word.length(); i++)
		{
            TrieNode child = root.getTrieNode(word.charAt(i));
            if(null == child)
            {
                child = new TrieNode();
                root.trieMap.put(word.charAt(i), child);
            }
			// root指向子节点，继续处理
			root = child;
		}
		root.end = true;
	}

	public void Insert(String word)
    {
	    Insert(this.root, word);
	}

	public TrieNode SearchNode(TrieNode root, String word)
	{
	    int len = word.length();
		for(int i = 0; i < len; i++)
		{	
			root = root.getTrieNode(word.charAt(i));
			if(null == root)
				return null;
		}
		return root;
	}
	
	public TrieNode SearchNode(String word)
    {
        return SearchNode(this.root, word);
    }
}
