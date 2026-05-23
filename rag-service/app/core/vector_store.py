import chromadb
from chromadb import Collection
from langchain_core.embeddings import Embeddings
from app.config import Settings

COLLECTIONS = ["knowledge_base", "user_profiles", "user_meal_history", "user_workout_history"]

_client: chromadb.ClientAPI | None = None


def get_chroma_client(settings: Settings) -> chromadb.ClientAPI:
    global _client
    if _client is None:
        _client = chromadb.PersistentClient(path=settings.CHROMA_PERSIST_DIR)
    return _client


def get_collection(client: chromadb.ClientAPI, name: str) -> Collection:
    return client.get_or_create_collection(
        name=name,
        metadata={"hnsw:space": "cosine"},
    )


class VectorStore:
    def __init__(self, settings: Settings, embeddings: Embeddings):
        self.client = get_chroma_client(settings)
        self.embeddings = embeddings
        self._collections: dict[str, Collection] = {}

    def _col(self, name: str) -> Collection:
        if name not in self._collections:
            self._collections[name] = get_collection(self.client, name)
        return self._collections[name]

    def upsert(self, collection: str, doc_id: str, text: str, metadata: dict) -> None:
        col = self._col(collection)
        embedding = self.embeddings.embed_query(text)
        col.upsert(
            ids=[doc_id],
            embeddings=[embedding],
            documents=[text],
            metadatas=[metadata],
        )

    def upsert_batch(
        self,
        collection: str,
        doc_ids: list[str],
        texts: list[str],
        metadatas: list[dict],
    ) -> None:
        col = self._col(collection)
        embeddings = self.embeddings.embed_documents(texts)
        col.upsert(
            ids=doc_ids,
            embeddings=embeddings,
            documents=texts,
            metadatas=metadatas,
        )

    def query(
        self,
        collection: str,
        query_text: str,
        n_results: int = 5,
        where: dict | None = None,
    ) -> list[str]:
        col = self._col(collection)
        count = col.count()
        if count == 0:
            return []
        actual_n = min(n_results, count)
        # Compute embedding ourselves so ChromaDB doesn't use its default (wrong dims)
        query_embedding = self.embeddings.embed_query(query_text)
        kwargs: dict = {"query_embeddings": [query_embedding], "n_results": actual_n}
        if where:
            kwargs["where"] = where
        results = col.query(**kwargs)
        docs = results.get("documents", [[]])[0]
        return docs if docs else []

    def count(self, collection: str) -> int:
        return self._col(collection).count()

    def delete_where(self, collection: str, where: dict) -> None:
        col = self._col(collection)
        results = col.get(where=where)
        if results["ids"]:
            col.delete(ids=results["ids"])


_vector_store: VectorStore | None = None


def get_vector_store() -> VectorStore:
    if _vector_store is None:
        raise RuntimeError("VectorStore not initialized — call init_vector_store() first")
    return _vector_store


def init_vector_store(settings: Settings, embeddings: Embeddings) -> VectorStore:
    global _vector_store
    _vector_store = VectorStore(settings, embeddings)
    return _vector_store
