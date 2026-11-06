"""Colonnes, thèmes d'explication et seuil de décision du modèle de churn."""

NUMERIC = ["tenure", "MonthlyCharges", "TotalCharges"]

CATEGORICAL = [
    "PhoneService", "MultipleLines", "InternetService", "OnlineSecurity",
    "OnlineBackup", "DeviceProtection", "TechSupport", "StreamingTV",
    "StreamingMovies", "Contract", "PaperlessBilling", "PaymentMethod",
]

FEATURES = NUMERIC + CATEGORICAL

# Les variables qui mesurent la même chose sont regroupées : TotalCharges suit
# l'ancienneté, MonthlyCharges suit les services pris. Pris séparément, leurs
# signes seraient trompeurs (double comptage).
GROUPS = {
    "Ancienneté": ["tenure", "TotalCharges"],
    "Contrat": ["Contract"],
    "Facturation": ["PaperlessBilling", "PaymentMethod"],
    "Internet, options et prix": [
        "InternetService", "OnlineSecurity", "OnlineBackup", "DeviceProtection",
        "TechSupport", "StreamingTV", "StreamingMovies", "MonthlyCharges",
    ],
    "Téléphone": ["PhoneService", "MultipleLines"],
}

GROUP_OF = {variable: group for group, variables in GROUPS.items() for variable in variables}

# Seuil choisi par analyse du gain métier (voir notebooks/02_modeling.ipynb)
THRESHOLD = 0.2