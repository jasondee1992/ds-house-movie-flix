from fastapi import APIRouter, Depends, Request
from sqlalchemy.orm import Session

from app.db.dependencies import get_session
from app.schemas.movie import ScanResponse
from app.services.media_scanner import scan_media_directories

router = APIRouter(prefix="/library", tags=["library"])


@router.post("/scan", response_model=ScanResponse)
def scan_library(request: Request, session: Session = Depends(get_session)) -> ScanResponse:
    result = scan_media_directories(session, request.app.state.settings.media_dirs)
    return ScanResponse(status="ok", **result.__dict__)
