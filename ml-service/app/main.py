"""API interne du service de prédiction de churn."""
from __future__ import annotations

import hmac
import os
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import Depends, FastAPI, Header, HTTPException, Request, status

from app.schemas import PredictRequest, PredictResponse
from app.scoring import ChurnScorer

DEFAULT_MODEL = Path(__file__).resolve().parents[1] / "models" / "churn_model.joblib"


def create_app(scorer: ChurnScorer | None = None, token: str | None = None) -> FastAPI:
    api_token = token or os.environ.get("ML_API_TOKEN", "")
    if len(api_token) < 32:
        raise RuntimeError("ML_API_TOKEN must be set and at least 32 characters long")

    docs_enabled = os.environ.get("ML_ENABLE_DOCS") == "true"

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        app.state.scorer = scorer or ChurnScorer.load(
            os.environ.get("MODEL_PATH", DEFAULT_MODEL)
        )
        yield

    app = FastAPI(
        title="ChurnGuard ML service",
        lifespan=lifespan,
        docs_url="/docs" if docs_enabled else None,
        redoc_url=None,
        openapi_url="/openapi.json" if docs_enabled else None,
    )

    def require_token(x_internal_token: str | None = Header(default=None)) -> None:
        if x_internal_token is None or not hmac.compare_digest(
            x_internal_token.encode(), api_token.encode()
        ):
            raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Invalid or missing token")

    @app.get("/health")
    def health() -> dict:
        return {"status": "ok"}

    @app.get("/model/info", dependencies=[Depends(require_token)])
    def model_info(request: Request) -> dict:
        model: ChurnScorer = request.app.state.scorer
        return {"threshold": model.threshold, "metrics": model.metrics}

    @app.post("/predict", response_model=PredictResponse, dependencies=[Depends(require_token)])
    def predict(body: PredictRequest, request: Request) -> PredictResponse:
        model: ChurnScorer = request.app.state.scorer
        rows = [customer.model_dump() for customer in body.customers]
        return PredictResponse(threshold=model.threshold, predictions=model.predict(rows))

    return app