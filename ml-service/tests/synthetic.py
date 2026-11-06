"""Jeu de données artificiel : mêmes colonnes que le jeu réel, signal simple à apprendre."""
import numpy as np
import pandas as pd


def make_dataframe(n: int = 800, seed: int = 0) -> pd.DataFrame:
    rng = np.random.default_rng(seed)

    tenure = rng.integers(0, 73, n)
    contract = rng.choice(["Month-to-month", "One year", "Two year"], n, p=[0.55, 0.25, 0.20])
    internet = rng.choice(["DSL", "Fiber optic", "No"], n, p=[0.35, 0.45, 0.20])
    phone = rng.choice(["Yes", "No"], n, p=[0.9, 0.1])
    monthly = np.round(rng.uniform(20, 110, n), 2)

    def addon():
        return np.where(internet == "No", "No internet service", rng.choice(["Yes", "No"], n))

    logit = (-0.8 + 1.4 * (contract == "Month-to-month")
             - 0.04 * tenure + 0.7 * (internet == "Fiber optic"))
    churn = rng.random(n) < 1 / (1 + np.exp(-logit))

    total = np.round(tenure * monthly, 2).astype(object)
    total[tenure == 0] = " "  # comme dans le CSV réel : texte vide

    return pd.DataFrame({
        "tenure": tenure,
        "MonthlyCharges": monthly,
        "TotalCharges": total,
        "PhoneService": phone,
        "MultipleLines": np.where(phone == "No", "No phone service", rng.choice(["Yes", "No"], n)),
        "InternetService": internet,
        "OnlineSecurity": addon(),
        "OnlineBackup": addon(),
        "DeviceProtection": addon(),
        "TechSupport": addon(),
        "StreamingTV": addon(),
        "StreamingMovies": addon(),
        "Contract": contract,
        "PaperlessBilling": rng.choice(["Yes", "No"], n),
        "PaymentMethod": rng.choice(
            ["Electronic check", "Mailed check",
             "Bank transfer (automatic)", "Credit card (automatic)"], n),
        "Churn": np.where(churn, "Yes", "No"),
    })