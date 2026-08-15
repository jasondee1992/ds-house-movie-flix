from fastapi import APIRouter

from app.api.routes import library, movies, system

api_router = APIRouter()
api_router.include_router(system.router)
api_router.include_router(movies.router)
api_router.include_router(library.router)
