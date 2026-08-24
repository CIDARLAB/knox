let gnnConfig = {
    //"seed":42,   // Determined in Frontend
    //"task": "classification", // Determined in Frontend
    "graph_conv": "gat", // "graph", "nn", "gat", "tconv"
    "hidden_dim": 128,
    "embedding_dim": 64,
    "num_layers": 3,
    "aggr": "mean",      // "sum", "mean", "max", "min"
    "dropout": 0.1,
    //"node_features_dim": 0, // Determined in Backend
    //"edge_dim": 0,         // Determined in Backend
    //"vocab_size": 19,  // Determined in Backend
    //"features_dim": 0, // Determined in Backend
    //"out_dim": 1,      // Determined in Backend
    "lr": 1e-3,
    "batch_size": 32,
    "max_epochs": 50,
    "num_workers": 0
};

let mlpConfig = {
    //"seed":42,   // Determined in Frontend
    //"task": "classification", // Determined in Frontend
    "hidden_dim": 128,
    "embedding_dim": 64,
    "num_layers": 5,
    "dropout": 0.1,
    //"sequence_length": 0, // Determined in Backend
    //"vocab_size": 19,  // Determined in Backend
    //"features_dim": 0, // Determined in Backend
    //"out_dim": 1,      // Determined in Backend
    "lr": 1e-4,
    "batch_size": 32,
    "max_epochs": 50,
    "num_workers": 0
};

let transformerConfig = {
    //"seed":42,   // Determined in Frontend
    //"task": "classification", // Determined in Frontend
    "model_dim": 128,
    "num_heads": 4,
    "num_layers": 3,
    "dropout": 0.1,
    //"vocab_size": 19,  // Determined in Backend
    //"features_dim": 0, // Determined in Backend
    //"out_dim": 1,      // Determined in Backend
    "lr": 1e-4,
    "batch_size": 32,
    "max_epochs": 50,
    "num_workers": 0
};

let rfConfig = {
    //"random_state": 42, // Determined in Frontend
    "n_estimators": 100,
    "max_depth": null,
    "min_samples_split": 2,
    "min_samples_leaf": 1,
    "max_features": "sqrt"
};

let xgboostConfig = {
    //"random_state": 42, // Determined in Frontend
    "n_estimators": 100,
    "max_depth": 4,
    "learning_rate": 0.1,
    "subsample": 0.8
};

let ebmConfig = {
    "interactions": 10,
    "outer_bags": 14,
    "inner_bags": 0,
    "learning_rate": 0.04,
    //"random_state": 42,  // Determined in Frontend
    "early_stopping_rounds": 10,
    "validation_size": 0.1
};

export let modelConfigs = {
    "gnn" : gnnConfig,
    "mlp" : mlpConfig,
    "transformer" : transformerConfig,
    "random_forest" : rfConfig,
    "xgboost" : xgboostConfig,
    "ebm" : ebmConfig
}