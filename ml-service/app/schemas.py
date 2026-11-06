"""Contrats d'entrée et de sortie de l'API (validation stricte)."""
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

YesNo = Literal["Yes", "No"]
InternetAddon = Literal["Yes", "No", "No internet service"]


class CustomerFeatures(BaseModel):
    model_config = ConfigDict(extra="forbid")

    tenure: int = Field(ge=0, le=1200)
    MonthlyCharges: float = Field(ge=0, le=99_999_999.99)
    TotalCharges: float | None = Field(default=None, ge=0, le=9_999_999_999.99)
    PhoneService: YesNo
    MultipleLines: Literal["Yes", "No", "No phone service"]
    InternetService: Literal["DSL", "Fiber optic", "No"]
    OnlineSecurity: InternetAddon
    OnlineBackup: InternetAddon
    DeviceProtection: InternetAddon
    TechSupport: InternetAddon
    StreamingTV: InternetAddon
    StreamingMovies: InternetAddon
    Contract: Literal["Month-to-month", "One year", "Two year"]
    PaperlessBilling: YesNo
    PaymentMethod: Literal[
        "Electronic check", "Mailed check",
        "Bank transfer (automatic)", "Credit card (automatic)",
    ]


class PredictRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    customers: list[CustomerFeatures] = Field(min_length=1, max_length=1000)


class Factor(BaseModel):
    theme: str
    impact: float
    direction: Literal["increases", "decreases"]


class Prediction(BaseModel):
    probability: float
    at_risk: bool
    factors: list[Factor]


class PredictResponse(BaseModel):
    threshold: float
    predictions: list[Prediction]