import pytest
from fastapi.testclient import TestClient

from app.features import GROUPS
from app.main import create_app


def post(client, customers, headers=None):
    return client.post("/predict", json={"customers": customers}, headers=headers or {})


def test_health_is_public(client):
    assert client.get("/health").status_code == 200


def test_predict_requires_a_valid_token(client, sample):
    assert post(client, [sample]).status_code == 401
    assert post(client, [sample], {"X-Internal-Token": "wrong"}).status_code == 401


def test_model_info_requires_a_valid_token(client, headers):
    assert client.get("/model/info").status_code == 401
    assert client.get("/model/info", headers=headers).status_code == 200


def test_valid_prediction_has_probability_decision_and_all_themes(client, sample, headers):
    response = post(client, [sample], headers)
    assert response.status_code == 200

    body = response.json()
    prediction = body["predictions"][0]
    assert 0 <= prediction["probability"] <= 1
    assert prediction["at_risk"] == (prediction["probability"] >= body["threshold"])
    assert {f["theme"] for f in prediction["factors"]} == set(GROUPS)


def test_missing_total_charges_is_accepted(client, sample, headers):
    sample["TotalCharges"] = None
    assert post(client, [sample], headers).status_code == 200


@pytest.mark.parametrize("field,value", [
    ("Contract", "Mensuel"),
    ("InternetService", "Satellite"),
    ("tenure", -1),
    ("MonthlyCharges", -5),
])
def test_invalid_values_are_rejected(client, sample, headers, field, value):
    sample[field] = value
    assert post(client, [sample], headers).status_code == 422


def test_sensitive_fields_cannot_be_sent(client, sample, headers):
    sample["gender"] = "Female"
    assert post(client, [sample], headers).status_code == 422


def test_empty_and_oversized_batches_are_rejected(client, sample, headers):
    assert post(client, [], headers).status_code == 422
    assert post(client, [sample] * 1001, headers).status_code == 422


def test_new_monthly_customer_is_riskier_than_loyal_two_year_customer(client, sample, headers):
    risky = {**sample, "tenure": 1, "Contract": "Month-to-month"}
    safe = {**sample, "tenure": 70, "Contract": "Two year"}

    predictions = post(client, [risky, safe], headers).json()["predictions"]
    assert predictions[0]["probability"] > predictions[1]["probability"]


def test_docs_are_disabled_by_default(scorer, monkeypatch):
    monkeypatch.delenv("ML_ENABLE_DOCS", raising=False)
    with TestClient(create_app(scorer=scorer, token="x" * 40)) as test_client:
        assert test_client.get("/docs").status_code == 404
        assert test_client.get("/openapi.json").status_code == 404


def test_service_refuses_to_start_without_a_strong_token(scorer, monkeypatch):
    monkeypatch.delenv("ML_API_TOKEN", raising=False)
    with pytest.raises(RuntimeError):
        create_app(scorer=scorer, token="short")
    with pytest.raises(RuntimeError):
        create_app(scorer=scorer)