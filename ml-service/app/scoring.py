"""Calcul du score de churn et des explications par thème."""
from __future__ import annotations

from pathlib import Path

import joblib
import numpy as np
import pandas as pd

from app.features import FEATURES, GROUP_OF, GROUPS

TOP_FACTORS = 5


def _variable_of(feature_name: str) -> str:
    """'cat__Contract_Two year' -> 'Contract' ; 'num__tenure' -> 'tenure'."""
    name = feature_name.split("__", 1)[1]
    matches = [v for v in FEATURES if name == v or name.startswith(v + "_")]
    return max(matches, key=len)


class ChurnScorer:
    def __init__(self, bundle: dict) -> None:
        self._pipeline = bundle["pipeline"]
        self._mean_t = bundle["mean_t"]
        self._threshold = bundle["threshold"]
        self.metrics = bundle["metrics"]

        self._prep = self._pipeline.named_steps["prep"]
        self._coef = self._pipeline.named_steps["clf"].coef_[0]

        groups = [GROUP_OF[_variable_of(name)] for name in bundle["feature_names"]]
        self._group_columns = {
            group: np.array([i for i, g in enumerate(groups) if g == group])
            for group in GROUPS
        }

    @classmethod
    def load(cls, path: str | Path) -> "ChurnScorer":
        return cls(joblib.load(Path(path)))

    @property
    def threshold(self) -> float:
        return self._threshold

    def predict(self, rows: list[dict]) -> list[dict]:
        df = pd.DataFrame(rows, columns=FEATURES)
        df["TotalCharges"] = pd.to_numeric(df["TotalCharges"], errors="coerce")

        probabilities = self._pipeline.predict_proba(df)[:, 1]
        contributions = self._coef * (np.asarray(self._prep.transform(df)) - self._mean_t)

        results = []
        for i, probability in enumerate(probabilities):
            effects = {
                group: float(contributions[i, columns].sum())
                for group, columns in self._group_columns.items()
            }
            ranked = sorted(effects.items(), key=lambda kv: abs(kv[1]), reverse=True)
            results.append({
                "probability": round(float(probability), 4),
                "at_risk": bool(probability >= self._threshold),
                "factors": [
                    {
                        "theme": theme,
                        "impact": round(effect, 2),
                        "direction": "increases" if effect > 0 else "decreases",
                    }
                    for theme, effect in ranked[:TOP_FACTORS]
                ],
            })
        return results