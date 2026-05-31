## Development is done in Java 21 targetting Quarkus ##

** use huggingface sentence-transformers

** expose rest endpoint for single embedding request

** expose JMS endpoint for batch embedding requests
** this service only does maths and makes no outbound calls, be as efficient as you can

** the motivation is to improve the performace from what we see using FastAPI python at:
~/apps/deepdivee/src/deepdive/agent/embedders.py

