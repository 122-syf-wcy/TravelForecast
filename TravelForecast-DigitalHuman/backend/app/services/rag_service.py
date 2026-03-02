"""
智教黔行 - RAG 知识检索引擎
检索增强生成服务
"""

import os
import json
import hashlib
import numpy as np
from typing import List, Dict, Optional, Tuple
from dataclasses import dataclass
import asyncio

from app.core.config import get_settings

settings = get_settings()


@dataclass
class Document:
    """知识文档结构"""
    id: str
    content: str
    metadata: Dict
    embedding: Optional[List[float]] = None


@dataclass
class SearchResult:
    """检索结果结构"""
    document: Document
    score: float
    source: str


class RAGService:
    """
    RAG 知识检索服务
    
    功能：
    - 本地知识库管理
    - 向量相似度检索
    - 混合检索（BM25 + 向量）
    - 上下文构建
    """
    
    # 六盘水研学知识库（内置）
    BUILTIN_KNOWLEDGE = [
        {
            "id": "geo_001",
            "title": "乌蒙大草原",
            "content": """乌蒙大草原位于贵州省六盘水市盘州市，是贵州省最大的草原，海拔约2400-2857米。
这里是喀斯特地貌与高原草甸的完美结合，被称为"南方的呼伦贝尔"。
由于海拔高、风力大，这里建设了风力发电场。风力发电机利用高原强劲的西风和季风，
将风能转化为电能。这种清洁能源的利用，既保护了草原生态，又为当地提供了绿色电力。
草原上的植被呈现典型的垂直地带性分布：2400米以下为针阔混交林，
2400-2600米为高山灌丛，2600米以上为高山草甸。""",
            "category": "地理",
            "keywords": ["乌蒙大草原", "风力发电", "垂直地带性", "高山草甸", "盘州"]
        },
        {
            "id": "geo_002", 
            "title": "梅花山滑雪场",
            "content": """梅花山位于六盘水市钟山区，海拔约2600米，是中国纬度最低的天然滑雪场。
每年冬季12月至次年3月，这里会形成天然积雪，可以开展滑雪运动。
梅花山的独特之处在于：虽然纬度低（约26°N），但由于海拔高，
气温随海拔每升高100米下降约0.6°C，使得山顶冬季气温可达-5°C以下。
这就是"高处不胜寒"的地理原理。滑雪场配有缆车、雪具租赁等设施。
冬季高峰期日均客流可达8000人次，建议错峰出行。""",
            "category": "地理",
            "keywords": ["梅花山", "滑雪", "海拔", "气温递减率", "纬度"]
        },
        {
            "id": "geo_003",
            "title": "玉舍国家森林公园",
            "content": """玉舍国家森林公园位于六盘水市水城区，是亚热带常绿阔叶林的典型代表。
公园海拔在1700-2500米之间，年均气温12.5°C，森林覆盖率达90%以上。
这里是珙桐、银杏等珍稀植物的栖息地。珙桐被称为"植物活化石"，
其白色苞片形似飞翔的鸽子，因此又名"鸽子树"。
公园内负氧离子含量极高，每立方厘米可达5万个，是名副其实的"天然氧吧"。
森林对调节气候、涵养水源、保持水土起着重要作用。""",
            "category": "生态",
            "keywords": ["玉舍森林公园", "珙桐", "负氧离子", "亚热带", "森林"]
        },
        {
            "id": "hist_001",
            "title": "三线建设历史",
            "content": """六盘水市因"三线建设"而兴起，是一座因煤而建的工业城市。
1964年，为了备战备荒，国家决定在西南腹地建设战略后方基地，
六盘水作为重要的煤炭资源基地被纳入三线建设规划。
水城钢铁厂、盘江煤电集团等大型企业相继建立。
"三线精神"包括：艰苦创业、无私奉献、团结协作、勇于创新。
如今，六盘水正在从传统煤炭城市向生态旅游城市转型，
打造"中国凉都"品牌，夏季平均气温仅19°C。""",
            "category": "历史",
            "keywords": ["三线建设", "煤炭", "水城钢铁", "中国凉都", "转型"]
        },
        {
            "id": "safety_001",
            "title": "研学安全须知",
            "content": """六盘水地区研学安全注意事项：
1. 高海拔防护：部分景区海拔超过2500米，可能出现高原反应，建议缓慢活动，多喝水。
2. 天气多变：山区天气变化快，需携带雨具和防寒衣物。
3. 地形安全：喀斯特地貌多溶洞、陡坡，请勿离开指定路线。
4. 野生动植物：不要触摸或采摘野生植物，部分可能有毒。
5. 紧急联系：景区救援电话110/119，游客中心电话0858-xxx-xxxx。
出现头晕、恶心等高反症状，应立即原地休息并通知带队老师。""",
            "category": "安全",
            "keywords": ["安全", "高原反应", "天气", "地形", "紧急"]
        },
        {
            "id": "carbon_001",
            "title": "碳足迹与低碳出行",
            "content": """碳足迹是指个人或活动产生的温室气体排放总量。研学活动中的碳足迹计算：
- 大巴车：每公里每人约0.03kg CO2
- 私家车：每公里每人约0.12kg CO2
- 步行/骑行：0 kg CO2
计算公式：碳排放 = 里程 × 单位排放系数 × e^(0.02×平均坡度)
山区道路坡度大，车辆能耗更高。选择公共交通、拼车出行可减少碳排放。
六盘水的风电、光伏等清洁能源占比逐年提高，
2025年可再生能源发电量预计达全市总发电量的40%。""",
            "category": "环保",
            "keywords": ["碳足迹", "低碳", "清洁能源", "环保", "计算"]
        }
    ]
    
    def __init__(self):
        # 知识库存储
        self.documents: Dict[str, Document] = {}
        
        # 简单的词频索引（BM25备用）
        self.inverted_index: Dict[str, List[str]] = {}
        
        # 嵌入模型（可选，使用OpenAI或本地模型）
        self.embedding_model = None
        
        # 加载内置知识
        self._load_builtin_knowledge()
        
        print(f"[RAG] 知识库初始化完成，共 {len(self.documents)} 条文档")
    
    def _load_builtin_knowledge(self):
        """加载内置知识库"""
        for item in self.BUILTIN_KNOWLEDGE:
            doc = Document(
                id=item["id"],
                content=f"{item['title']}\n\n{item['content']}",
                metadata={
                    "title": item["title"],
                    "category": item["category"],
                    "keywords": item["keywords"]
                }
            )
            self.documents[doc.id] = doc
            
            # 构建倒排索引
            self._index_document(doc)
    
    def _index_document(self, doc: Document):
        """为文档构建倒排索引"""
        # 简单分词（中文按字，英文按空格）
        words = self._tokenize(doc.content)
        keywords = doc.metadata.get("keywords", [])
        
        for word in set(words + keywords):
            if word not in self.inverted_index:
                self.inverted_index[word] = []
            if doc.id not in self.inverted_index[word]:
                self.inverted_index[word].append(doc.id)
    
    def _tokenize(self, text: str) -> List[str]:
        """简单分词"""
        import re
        # 中文逐字 + 英文按词
        chinese = re.findall(r'[\u4e00-\u9fff]+', text)
        english = re.findall(r'[a-zA-Z]+', text.lower())
        
        result = []
        for c in chinese:
            result.extend(list(c))  # 中文逐字
        result.extend(english)
        
        return result
    
    async def search(
        self, 
        query: str, 
        top_k: int = 3,
        method: str = "hybrid"
    ) -> List[SearchResult]:
        """
        检索相关文档
        
        Args:
            query: 查询文本
            top_k: 返回结果数量
            method: 检索方法 (keyword/vector/hybrid)
            
        Returns:
            检索结果列表
        """
        results = []
        
        if method in ["keyword", "hybrid"]:
            # 关键词检索（BM25简化版）
            keyword_results = self._keyword_search(query, top_k * 2)
            results.extend(keyword_results)
        
        if method in ["vector", "hybrid"]:
            # 向量检索（如果可用）
            vector_results = await self._vector_search(query, top_k * 2)
            results.extend(vector_results)
        
        # 去重并排序
        seen_ids = set()
        unique_results = []
        for r in sorted(results, key=lambda x: x.score, reverse=True):
            if r.document.id not in seen_ids:
                seen_ids.add(r.document.id)
                unique_results.append(r)
        
        return unique_results[:top_k]
    
    def _keyword_search(self, query: str, top_k: int) -> List[SearchResult]:
        """关键词检索"""
        query_tokens = self._tokenize(query)
        doc_scores: Dict[str, float] = {}
        
        for token in query_tokens:
            if token in self.inverted_index:
                for doc_id in self.inverted_index[token]:
                    if doc_id not in doc_scores:
                        doc_scores[doc_id] = 0
                    doc_scores[doc_id] += 1
        
        # 按分数排序
        sorted_docs = sorted(doc_scores.items(), key=lambda x: x[1], reverse=True)
        
        results = []
        for doc_id, score in sorted_docs[:top_k]:
            if doc_id in self.documents:
                results.append(SearchResult(
                    document=self.documents[doc_id],
                    score=score / len(query_tokens),  # 归一化
                    source="keyword"
                ))
        
        return results
    
    async def _vector_search(self, query: str, top_k: int) -> List[SearchResult]:
        """向量检索（基于TF-IDF + 余弦相似度）"""
        if not self.documents:
            return []

        # 构建所有文档的词集合
        all_docs = list(self.documents.values())
        all_tokens = [set(self._tokenize(doc.content)) for doc in all_docs]
        query_tokens = set(self._tokenize(query))

        # 构建词汇表
        vocab = set()
        for tokens in all_tokens:
            vocab.update(tokens)
        vocab.update(query_tokens)
        vocab_list = sorted(vocab)
        word_to_idx = {w: i for i, w in enumerate(vocab_list)}

        # 计算IDF
        num_docs = len(all_docs)
        idf = np.zeros(len(vocab_list))
        for i, word in enumerate(vocab_list):
            df = sum(1 for tokens in all_tokens if word in tokens)
            idf[i] = np.log((num_docs + 1) / (df + 1)) + 1

        # 查询向量 (TF-IDF)
        query_vec = np.zeros(len(vocab_list))
        for token in query_tokens:
            if token in word_to_idx:
                query_vec[word_to_idx[token]] = 1.0 * idf[word_to_idx[token]]

        query_norm = np.linalg.norm(query_vec)
        if query_norm == 0:
            return []

        # 计算每个文档的相似度
        results = []
        for idx, doc in enumerate(all_docs):
            doc_vec = np.zeros(len(vocab_list))
            for token in all_tokens[idx]:
                if token in word_to_idx:
                    doc_vec[word_to_idx[token]] = 1.0 * idf[word_to_idx[token]]

            doc_norm = np.linalg.norm(doc_vec)
            if doc_norm == 0:
                continue

            similarity = float(np.dot(query_vec, doc_vec) / (query_norm * doc_norm))
            if similarity > 0.05:
                results.append(SearchResult(
                    document=doc,
                    score=similarity,
                    source="vector"
                ))

        results.sort(key=lambda x: x.score, reverse=True)
        return results[:top_k]
    
    async def retrieve_and_format(
        self, 
        query: str, 
        top_k: int = 3
    ) -> Tuple[str, List[str]]:
        """
        检索并格式化上下文
        
        Args:
            query: 用户问题
            top_k: 检索文档数
            
        Returns:
            (格式化的上下文, 来源列表)
        """
        results = await self.search(query, top_k)
        
        if not results:
            return "", []
        
        # 构建上下文
        context_parts = []
        sources = []
        
        for i, r in enumerate(results, 1):
            title = r.document.metadata.get("title", "未知")
            category = r.document.metadata.get("category", "通用")
            
            context_parts.append(f"【参考资料{i}】《{title}》（{category}类）\n{r.document.content}")
            sources.append(f"《{title}》")
        
        context = "\n\n---\n\n".join(context_parts)
        
        return context, sources
    
    def add_document(
        self, 
        content: str, 
        title: str = "", 
        category: str = "通用",
        keywords: List[str] = None
    ) -> str:
        """添加新文档到知识库"""
        doc_id = hashlib.md5(content.encode()).hexdigest()[:12]
        
        doc = Document(
            id=doc_id,
            content=f"{title}\n\n{content}" if title else content,
            metadata={
                "title": title or f"文档{doc_id}",
                "category": category,
                "keywords": keywords or []
            }
        )
        
        self.documents[doc_id] = doc
        self._index_document(doc)
        
        print(f"[RAG] 添加文档: {doc_id}")
        return doc_id
    
    def get_document(self, doc_id: str) -> Optional[Document]:
        """获取指定文档"""
        return self.documents.get(doc_id)
    
    def list_documents(self) -> List[Dict]:
        """列出所有文档"""
        return [
            {
                "id": doc.id,
                "title": doc.metadata.get("title", ""),
                "category": doc.metadata.get("category", ""),
                "preview": doc.content[:100] + "..."
            }
            for doc in self.documents.values()
        ]


# 单例
rag_service = RAGService()
