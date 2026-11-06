import pytest
from fastapi.testclient import TestClient

from app.main import create_app
from app.scoring import ChurnScorer
from app.training import train_bundle
from tests.synthetic import make_dataframe

TOKEN = "test-token-" + "a" * 40

SAMPLE_CUSTOMER = {
    "tenure": 12, "MonthlyCharges": 70.5, "TotalCharges": 846.0,
    "PhoneService": "Yes", "MultipleLines": "No", "InternetService": "Fiber optic",
    "OnlineSecurity": "No", "OnlineBackup": "No", "DeviceProtection": "No",
    "TechSupport": "No", "StreamingTV": "No", "StreamingMovies": "No",
    "Contract": "Month-to-month", "PaperlessBilling": "Yes",
    "PaymentMethod": "Electronic check",
}


@pytest.fixture(scope="session")
def bundle():
    return train_bundle(make_dataframe())


@pytest.fixture(scope="session")
def scorer(bundle):
    return ChurnScorer(bundle)


@pytest.fixture
def client(scorer):
    with TestClient(create_app(scorer=scorer, token=TOKEN)) as test_client:
        yield test_client


@pytest.fixture
def headers():
    return {"X-Internal-Token": TOKEN}


@pytest.fixture
def sample():
    return dict(SAMPLE_CUSTOMER)