"""Entraîne le modèle de churn et écrit le bundle et ses métriques.

Usage (depuis le dossier ml-service) :
    python -m app.training [chemin/vers/le/fichier.csv]
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
import sklearn
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import average_precision_score, precision_score, recall_score, roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from app.features import CATEGORICAL, FEATURES, NUMERIC, THRESHOLD

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CSV = ROOT.parent / "data" / "raw" / "WA_Fn-UseC_-Telco-Customer-Churn.csv"
MODELS_DIR = ROOT / "models"


def build_pipeline() -> Pipeline:
    preprocess = ColumnTransformer(
        [
            ("num", Pipeline([
                ("impute", SimpleImputer(strategy="constant", fill_value=0)),
                ("scale", StandardScaler()),
            ]), NUMERIC),
            ("cat", OneHotEncoder(handle_unknown="ignore"), CATEGORICAL),
        ],
        sparse_threshold=0,
    )
    return Pipeline([("prep", preprocess), ("clf", LogisticRegression(max_iter=1000))])


def prepare(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.Series]:
    df = df.copy()
    df["TotalCharges"] = pd.to_numeric(df["TotalCharges"], errors="coerce")
    y = (df["Churn"] == "Yes").astype(int)
    return df[FEATURES], y


def train_bundle(df: pd.DataFrame, seed: int = 42) -> dict:
    X, y = prepare(df)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, stratify=y, random_state=seed
    )

    pipeline = build_pipeline().fit(X_train, y_train)

    proba = pipeline.predict_proba(X_test)[:, 1]
    pred = (proba >= THRESHOLD).astype(int)
    metrics = {
        "threshold": THRESHOLD,
        "auc": round(float(roc_auc_score(y_test, proba)), 3),
        "auc_pr": round(float(average_precision_score(y_test, proba)), 3),
        "recall": round(float(recall_score(y_test, pred)), 3),
        "precision": round(float(precision_score(y_test, pred)), 3),
        "n_train": int(len(X_train)),
        "n_test": int(len(X_test)),
        "sklearn_version": sklearn.__version__,
    }

    # Client « moyen » dans l'espace transformé : référence des explications
    mean_t = np.asarray(pipeline.named_steps["prep"].transform(X_train)).mean(axis=0)

    return {
        "pipeline": pipeline,
        "mean_t": mean_t,
        "feature_names": list(pipeline.named_steps["prep"].get_feature_names_out()),
        "threshold": THRESHOLD,
        "metrics": metrics,
    }


def main() -> None:
    csv_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_CSV
    bundle = train_bundle(pd.read_csv(csv_path))

    MODELS_DIR.mkdir(exist_ok=True)
    joblib.dump(bundle, MODELS_DIR / "churn_model.joblib")
    (MODELS_DIR / "metrics.json").write_text(
        json.dumps(bundle["metrics"], indent=2), encoding="utf-8"
    )
    print(json.dumps(bundle["metrics"], indent=2))


if __name__ == "__main__":
    main()