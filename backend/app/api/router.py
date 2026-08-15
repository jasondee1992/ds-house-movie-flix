from fastapi import APIRouter

from app.api.routes import library, movies, progress, system

api_router = APIRouter()
api_router.include_router(system.router)
api_router.include_router(movies.router)
api_router.include_router(progress.router)
api_router.include_router(library.router)
