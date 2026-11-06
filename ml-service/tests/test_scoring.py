import numpy as np
import pandas as pd

from app.features import FEATURES, GROUPS
from tests.synthetic import make_dataframe


def sample_rows(n=50):
    df = make_dataframe(n=n, seed=1)
    df["TotalCharges"] = pd.to_numeric(df["TotalCharges"], errors="coerce")
    return df[FEATURES].to_dict(orient="records")


def test_scorer_probabilities_match_the_pipeline(bundle, scorer):
    rows = sample_rows()
    expected = bundle["pipeline"].predict_proba(pd.DataFrame(rows, columns=FEATURES))[:, 1]
    actual = [p["probability"] for p in scorer.predict(rows)]
    assert np.allclose(actual, expected, atol=1e-4)


def test_factors_are_sorted_and_directions_match_signs(scorer):
    for prediction in scorer.predict(sample_rows()):
        impacts = [f["impact"] for f in prediction["factors"]]
        assert len(impacts) == len(GROUPS)
        assert impacts == sorted(impacts, key=abs, reverse=True)
        for factor in prediction["factors"]:
            if factor["impact"] != 0:
                assert (factor["direction"] == "increases") == (factor["impact"] > 0)


def test_model_learns_the_synthetic_signal(bundle):
    assert bundle["metrics"]["auc"] > 0.7