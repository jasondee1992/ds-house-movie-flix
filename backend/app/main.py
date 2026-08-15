from fastapi import FastAPI

from app.api.router import api_router
from app.core.config import Settings, settings
from app.db.database import Database


def create_app(app_settings: Settings = settings) -> FastAPI:
    application = FastAPI(
        title=app_settings.api_name,
        version=app_settings.version,
        description="Local-network API for HomeFlix.",
    )
    application.state.settings = app_settings
    application.state.database = Database(app_settings.database_url)
    application.state.database.create_tables()
    application.include_router(api_router, prefix="/api")
    return application


app = create_app()
