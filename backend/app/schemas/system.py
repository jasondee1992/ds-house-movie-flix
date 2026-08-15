from typing import Literal

from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: Literal["ok"]
    service: str


class VersionResponse(BaseModel):
    name: str
    version: str

