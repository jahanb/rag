package com.example.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Groq groq = new Groq();
    private Embedding embedding = new Embedding();
    private Mongodb mongodb = new Mongodb();
    private Rag rag = new Rag();
    private Cors cors = new Cors();

    public Groq getGroq() { return groq; }
    public void setGroq(Groq v) { this.groq = v; }
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding v) { this.embedding = v; }
    public Mongodb getMongodb() { return mongodb; }
    public void setMongodb(Mongodb v) { this.mongodb = v; }
    public Rag getRag() { return rag; }
    public void setRag(Rag v) { this.rag = v; }
    public Cors getCors() { return cors; }
    public void setCors(Cors v) { this.cors = v; }

    public static class Groq {
        private String apiKey;
        private String baseUrl = "https://api.groq.com/openai/v1";
        private String model = "llama-3.3-70b-versatile";
        private int maxTokens = 2048;
        private double temperature = 0.2;
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
        public String getModel() { return model; }
        public void setModel(String v) { this.model = v; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int v) { this.maxTokens = v; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double v) { this.temperature = v; }
    }

    public static class Embedding {
        private String modelName = "sentence-transformers/all-MiniLM-L6-v2";
        private int dimensions = 384;
        public String getModelName() { return modelName; }
        public void setModelName(String v) { this.modelName = v; }
        public int getDimensions() { return dimensions; }
        public void setDimensions(int v) { this.dimensions = v; }
    }

    public static class Mongodb {
        private Vector vector = new Vector();
        public Vector getVector() { return vector; }
        public void setVector(Vector v) { this.vector = v; }

        public static class Vector {
            private String collection = "document_embeddings";
            private String indexName = "vector_index";
            private int dimensions = 384;
            private int numCandidates = 150;
            private int topK = 5;
            public String getCollection() { return collection; }
            public void setCollection(String v) { this.collection = v; }
            public String getIndexName() { return indexName; }
            public void setIndexName(String v) { this.indexName = v; }
            public int getDimensions() { return dimensions; }
            public void setDimensions(int v) { this.dimensions = v; }
            public int getNumCandidates() { return numCandidates; }
            public void setNumCandidates(int v) { this.numCandidates = v; }
            public int getTopK() { return topK; }
            public void setTopK(int v) { this.topK = v; }
        }
    }

    public static class Rag {
        private int chunkSize = 1500;
        private int chunkOverlap = 200;
        private int maxContextDocs = 5;
        private boolean useWebSearch = false;
        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int v) { this.chunkSize = v; }
        public int getChunkOverlap() { return chunkOverlap; }
        public void setChunkOverlap(int v) { this.chunkOverlap = v; }
        public int getMaxContextDocs() { return maxContextDocs; }
        public void setMaxContextDocs(int v) { this.maxContextDocs = v; }
        public boolean isUseWebSearch() { return useWebSearch; }
        public void setUseWebSearch(boolean v) { this.useWebSearch = v; }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String v) { this.allowedOrigins = v; }
    }
}