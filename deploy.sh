#!/bin/bash
set -e

# Configuración
STACK_NAME="employee-api"
REGION="us-east-1"  # Cambia según tu región de AWS
BUCKET_NAME="employee-api-deployment-bucket-$(date +%s)"

echo "Creando bucket S3 para despliegue..."
aws s3 mb s3://$BUCKET_NAME --region $REGION

echo "Compilando y empaquetando el proyecto con Maven..."
mvn clean package

echo "Subiendo el JAR a S3..."
aws s3 cp target/employee-api-1.0-SNAPSHOT.jar s3://$BUCKET_NAME/employee-api.jar

echo "Desplegando stack de CloudFormation..."
aws cloudformation deploy \
  --template-file cloudformation.yaml \
  --stack-name $STACK_NAME \
  --capabilities CAPABILITY_IAM \
  --parameter-overrides \
    LambdaCodeS3Bucket=$BUCKET_NAME \
    LambdaCodeS3Key=employee-api.jar \
  --region $REGION

echo "Obteniendo URL de la API..."
API_URL=$(aws cloudformation describe-stacks \
  --stack-name $STACK_NAME \
  --query "Stacks[0].Outputs[?OutputKey=='ApiEndpoint'].OutputValue" \
  --output text \
  --region $REGION)

echo "Creando usuario admin para pruebas..."
aws dynamodb put-item \
  --table-name Users \
  --item '{"username": {"S": "admin"}, "password": {"S": "admin123"}, "id": {"S": "admin-id"}}' \
  --region $REGION

echo ""
echo "============================================"
echo "Despliegue completado con éxito!"
echo "URL de la API: $API_URL"
echo "Usuario para pruebas: admin / admin123"
echo "============================================"
echo ""
echo "Ejemplos de uso:"
echo ""
echo "1. Login:"
echo "curl -X POST $API_URL/login -H 'Content-Type: application/json' -d '{\"username\":\"admin\",\"password\":\"admin123\"}'"
echo ""
echo "2. Crear empleado (reemplaza TOKEN con el token obtenido del login):"
echo "curl -X POST $API_URL/employees -H 'Content-Type: application/json' -H 'Authorization: TOKEN' -d '{\"nombre\":\"Juan Perez\",\"email\":\"juan@example.com\"}'"
echo ""
echo "3. Obtener todos los empleados:"
echo "curl -X GET $API_URL/employees -H 'Authorization: TOKEN'"
echo ""
echo "4. Obtener empleado por ID (reemplaza ID con el ID del empleado):"
echo "curl -X GET $API_URL/employees/ID -H 'Authorization: TOKEN'"