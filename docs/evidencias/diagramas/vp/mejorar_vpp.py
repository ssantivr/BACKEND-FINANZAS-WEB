# Edits the Visual Paradigm XML export (project.xml) to align CTX-01, FUN-01, LOG-01 and DEP-01 with the real code.
# usage: python mejorar.py in.xml out.xml
import copy
import itertools
import sys
import xml.etree.ElementTree as ET

NS = 'http://www.visual-paradigm.com/product/vpuml/modelexporter'
ET.register_namespace('', NS)
ET.register_namespace('xsi', 'http://www.w3.org/2001/XMLSchema-instance')
T = lambda tag: '{%s}%s' % (NS, tag)

src, dst = sys.argv[1], sys.argv[2]
tree = ET.parse(src)
root = tree.getroot()
parent = {c: p for p in root.iter() for c in p}
_contador = itertools.count(1)


def nuevo_id():
    return 'PFx%012dZ' % next(_contador)


def por_id(i, tag=None):
    for e in root.iter(tag and T(tag)):
        if e.get('id') == i:
            return e
    raise KeyError(i)


def prop(e, name):
    """Direct property element (…Properties/XProperty name=…) of a model or shape."""
    for p in e:
        if p.tag in (T('ModelProperties'), T('DiagramElementProperties')):
            for q in p:
                if q.get('name') == name:
                    return q
    return None


def renombrar(model_id, nombre):
    m = por_id(model_id, 'Model')
    m.set('name', nombre)
    p = prop(m, 'name')
    if p is not None:
        p.set('value', nombre)
    for e in root.iter():
        if e.tag in (T('Shape'), T('Connector'), T('DiagramElementRef')) and e.get('model') == model_id:
            e.set('name', nombre)


def eliminar(ids):
    ids = set(ids)
    for p in list(root.iter()):
        for c in list(p):
            if c.get('id') in ids:
                p.remove(c)
    # Drop dangling references.
    for p in list(root.iter()):
        for c in list(p):
            if c.tag in (T('ModelRef'), T('RelationshipRef'), T('DiagramElementRef')) and (
                    c.get('id') in ids or c.get('model') in ids):
                p.remove(c)


# ---------------------------------------------------------------- CTX-01
# The XML import merges into the existing project and cannot delete elements, so the old context diagram is kept
# as OBSOLETO and a clean copy (without the backup storage the code never implemented) becomes CTX-01.
ctx = next(d for d in root.iter(T('Diagram')) if d.get('name') == 'CTX-01 Contexto del sistema')
ctx_nuevo = copy.deepcopy(ctx)
for sec in (ctx_nuevo.find(T('Shapes')), ctx_nuevo.find(T('Connectors'))):
    for c in list(sec):
        if c.get('id') in ('FDx00000000001eZ', 'FDx000000000020Z'):
            sec.remove(c)
mapa = {}
for e in ctx_nuevo.iter():
    if e.tag in (T('Diagram'), T('Shape'), T('Connector')):
        mapa[e.get('id')] = nuevo_id()
        e.set('id', mapa[e.get('id')])
for c in ctx_nuevo.iter(T('Connector')):
    c.set('from', mapa[c.get('from')])
    c.set('to', mapa[c.get('to')])
ctx.set('name', 'OBSOLETO – CTX-01 Contexto (incluía respaldos, ya no aplica)')
cont = parent[ctx]
cont.insert(list(cont).index(ctx), ctx_nuevo)
renombrar('FDx00000000001aZ', 'Enlace de recuperación y alerta de presupuesto excedido\n'
                              '(HU-05, HU-15 · SMTP con STARTTLS, opcional)')

# ---------------------------------------------------------------- FUN-01
for mid, nombre in {
    'Cqypp4WGAqACAw6E': 'IGestionCategorias', 'aqypp4WGAqACAw6G': 'IGestionMovimientos',
    'mqypp4WGAqACAw6I': 'IGestionPresupuestos', 'Wqypp4WGAqACAw6K': 'IConsultaFinanciera',
    '2qypp4WGAqACAw6M': 'IAutenticacion', 'uqypp4WGAqACAw6O': 'IEvaluadorPresupuesto',
    'eqypp4WGAqACAw6Q': 'INotificador', 'Rqypp4WGAqACAw6U': 'IRepositorioCategorias',
    'Rqypp4WGAqACAw6V': 'IRepositorioMovimientos', 'xqypp4WGAqACAw6W': 'IRepositorioPresupuestos',
    'Jqypp4WGAqACAw6X': 'IRepositorioUsuarios', 'Zqypp4WGAqACAw6c': 'ICifradorContrasena',
    'Zqypp4WGAqACAw6d': 'IEmisorTokens', 'WHJtp4WGAqACAw83': 'IGestionCuenta',
    'Vqypp4WGAqACAw6g': 'MongoDB', 'Nmypp4WGAqACAw_q': ':27017',
    'Yqypp4WGAqACAw5_': 'MongoDB', 'Jmypp4WGAqACAw_X': 'STARTTLS',
    'FDx000000000026Z': 'FRONTEND WEB\n(React + TypeScript)',
}.items():
    renombrar(mid, nombre)

for f in root.iter(T('Shape')):
    if f.get('model') == 'Vqypp4WGAqACAw6g':   # MongoDB interface label: give the caption room
        cap = f.find(T('Caption'))
        cap.set('width', '110')
        cap.set('internalWidth', '110')

# ---------------------------------------------------------------- LOG-01: Categoria gets its real "color" attribute
categoria = None
for m in root.iter(T('Model')):
    if m.get('modelType') == 'Class' and m.get('name') == 'Categoria':
        hijos = m.find(T('ChildModels'))
        if hijos is not None and any(a.get('name') == 'icono' for a in hijos):
            categoria = m
icono = next(a for a in categoria.find(T('ChildModels')) if a.get('name') == 'icono')
color = copy.deepcopy(icono)
for e in color.iter():
    if e.get('id') and e.tag == T('Model'):
        e.set('id', nuevo_id())
color.set('name', 'color')
prop(color, 'name').set('value', 'color')
for e in color.iter(T('DiagramElementRefProperty')):
    for c in list(e):
        e.remove(c)
hijos = categoria.find(T('ChildModels'))
hijos.insert(list(hijos).index(icono) + 1, color)

# ---------------------------------------------------------------- DEP: old diagram becomes OBSOLETO, two new ones
dep_viejo = next(d for d in root.iter(T('Diagram')) if d.get('name') == 'DEP-01 Despliegue')
dep_viejo.set('name', 'OBSOLETO – DEP-01 Despliegue (PostgreSQL y Docker, ya no aplica)')

TPL_MODELO = {'device': 'FDx000000000216Z', 'env': 'FDx000000000218Z', 'artifact': 'FDx000000000224Z',
              'node': 'FDx000000000221Z'}
TPL_FORMA = {'device': 'FDx00000000022dZ', 'env': 'FDx00000000022eZ', 'artifact': 'FDx00000000022fZ',
             'node': 'FDx00000000023dZ'}
TPL_ASOC, TPL_CONECTOR = 'FDx00000000023fZ', 'FDx000000000242Z'
ESTEREOTIPO = {'device': 'FDx000000000217Z', 'env': 'FDx000000000219Z', 'HTTPS': 'DPx000000000027Z',
               'HTTP': 'DPx000000000028Z', 'SMTP': 'DPx00000000002aZ'}

# «node» stereotype (UML) cloned from «device».
st_node = copy.deepcopy(por_id(ESTEREOTIPO['device'], 'Model'))
for e in st_node.iter(T('Model')):
    e.set('id', nuevo_id())
st_node.set('name', 'node')
prop(st_node, 'name').set('value', 'node')
parent[por_id(ESTEREOTIPO['device'], 'Model')].append(st_node)
ESTEREOTIPO['node'] = st_node.get('id')

contenedor_modelos = parent[por_id(TPL_MODELO['device'], 'Model')]
contenedor_asoc = parent[por_id(TPL_ASOC, 'Model')]
contenedor_diagramas = parent[dep_viejo]


def limpiar_modelo(m):
    for tag in ('ChildModels', 'FromEndRelationships', 'ToEndRelationships'):
        for c in m.findall(T(tag)):
            m.remove(c)
    tv = prop(m, 'taggedValues')
    if tv is not None:
        for c in list(tv):
            tv.remove(c)
    mv = prop(m, 'masterView')
    if mv is not None:
        for c in list(mv):
            mv.remove(c)


def poner_estereotipo(m, clave):
    st = prop(m, 'stereotypes')
    if st is None:
        return
    for c in list(st):
        st.remove(c)
    if clave:
        ET.SubElement(st, T('ModelRef'), {'id': ESTEREOTIPO[clave]})


def poner_prop(forma, nombre, valor):
    p = prop(forma, nombre)
    if p is not None:
        p.set('value', str(valor))


TPL_CONT_TV, TPL_TV = 'DPx000000000001Z', 'DPx000000000002Z'


def documentar(m, texto):
    p = prop(m, 'documentation')
    if p is None:
        return
    html = ''.join('<p style="margin-top: 0">%s</p>' % (linea.replace('&', '&amp;').replace('<', '&lt;')
                                                         .replace('>', '&gt;'))
                   for linea in texto.split('\n'))
    p.set('plainTextValue', texto)
    p.set('value', '<HTML><BODY>%s</BODY></HTML>' % html)


def etiquetas(m, pares):
    """UML tagged values (text) on a model, cloned from the project's own tagged value template."""
    p = prop(m, 'taggedValues')
    if p is None or not pares:
        return
    for c in list(p):
        p.remove(c)
    cont = copy.deepcopy(por_id(TPL_CONT_TV, 'Model'))
    cont.set('id', nuevo_id())
    hijos = cont.find(T('ChildModels'))
    for c in list(hijos):
        hijos.remove(c)
    for clave, valor in pares:
        tv = copy.deepcopy(por_id(TPL_TV, 'Model'))
        tv.set('id', nuevo_id())
        tv.set('name', clave)
        prop(tv, 'name').set('value', clave)
        prop(tv, 'value').find(T('StringValue')).set('value', valor)
        hijos.append(tv)
    p.append(cont)


class Diagrama:
    def __init__(self, nombre, doc=''):
        self.d = copy.deepcopy(dep_viejo)
        self.d.set('id', nuevo_id())
        self.d.set('name', nombre)
        self.d.set('documentation', doc)
        for p in self.d.find(T('DiagramProperties')):
            if p.get('name') == 'showTaggedValues':
                p.set('value', 'true')
            if p.get('name') == 'hideEmptyTaggedValues':
                p.set('value', 'true')
            if p.get('name') == 'documentation':
                p.set('value', doc)
        for tag in ('Shapes', 'Connectors'):
            sec = self.d.find(T(tag))
            for c in list(sec):
                sec.remove(c)
        contenedor_diagramas.append(self.d)
        self.formas = {}
        self.z = itertools.count(2, 2)

    def nodo(self, clave, tipo, nombre, x, y, w, h, dentro=None, estereotipo=None, relleno=None, tv=None, doc=None):
        m = copy.deepcopy(por_id(TPL_MODELO[tipo], 'Model'))
        limpiar_modelo(m)
        etiquetas(m, tv)
        if doc:
            documentar(m, doc)
        m.set('id', nuevo_id())
        for e in m.iter(T('Model')):
            if e is not m:
                e.set('id', nuevo_id())
        m.set('name', nombre)
        prop(m, 'name').set('value', nombre)
        if tipo != 'artifact':
            poner_estereotipo(m, estereotipo or {'device': 'device', 'env': 'env', 'node': 'node'}[tipo])
        if dentro:
            pm = self.formas[dentro][1]
            hijos = pm.find(T('ChildModels'))
            if hijos is None:
                hijos = ET.Element(T('ChildModels'))
                pm.insert(1, hijos)
            hijos.append(m)
        else:
            contenedor_modelos.append(m)

        f = copy.deepcopy(por_id(TPL_FORMA[tipo], 'Shape'))
        for c in f.findall(T('ChildShapes')):
            f.remove(c)
        z = next(self.z)
        f.attrib.update(id=nuevo_id(), model=m.get('id'), name=nombre, x=str(x), y=str(y), width=str(w),
                        height=str(h), zorder=str(z))
        for k, v in (('x', x), ('y', y), ('width', w), ('height', h), ('zOrder', z)):
            poner_prop(f, k, v)
        # Fill colour by kind: devices white, execution environments light blue, managed services light grey,
        # artifacts white (the professor's examples are plain; a light tint tells the levels apart).
        color = relleno or {'device': '255,255,255', 'env': '232,243,252', 'node': '242,242,242',
                            'artifact': '255,255,255'}[tipo]
        poner_prop(f, 'background', 'Cr:%s,255' % color)
        fc = f.find(T('FillColor'))
        if fc is not None:
            fc.set('color', 'Cr:%s,255' % color)
        mm = prop(f, 'metaModelElement')
        for c in list(mm):
            mm.remove(c)
        ET.SubElement(mm, T('ModelRef'), {'id': m.get('id')})
        poner_prop(f, 'requestResetCaption', 'true')
        cap = f.find(T('Caption'))
        if cap is not None:
            if tipo == 'artifact':   # artifact captions are relative to the shape
                cap.attrib.update(x='0', y='0', width=str(w - 24), height=str(h))
            else:
                cap.attrib.update(x=str(x), y=str(y + 8), width=str(w))
        mv = prop(m, 'masterView')
        if mv is not None:
            ET.SubElement(mv, T('DiagramElementRef'), {'displayShapeType': f.get('shapeType'), 'id': f.get('id'),
                                                      'model': m.get('id'), 'name': nombre,
                                                      'shapeType': f.get('shapeType')})
        if dentro:
            pf = self.formas[dentro][0]
            hs = pf.find(T('ChildShapes'))
            if hs is None:
                hs = ET.Element(T('ChildShapes'))
                pf.insert(list(pf).index(pf.find(T('Caption'))) if pf.find(T('Caption')) is not None else len(pf), hs)
            hs.append(f)
        else:
            self.d.find(T('Shapes')).append(f)
        self.formas[clave] = (f, m, (x, y, w, h))

    def ruta(self, desde, hasta, nombre, estereotipo=None, puntos=None):
        fa, ma, ra = self.formas[desde]
        fb, mb, rb = self.formas[hasta]
        a = copy.deepcopy(por_id(TPL_ASOC, 'Model'))
        a.set('id', nuevo_id())
        for e in a.iter(T('Model')):
            if e is not a:
                e.set('id', nuevo_id())
        a.set('name', nombre)
        prop(a, 'name').set('value', nombre)
        for pn, mid in (('endRelationshipFromMetaModelElement', ma), ('endRelationshipToMetaModelElement', mb)):
            p = prop(a, pn)
            p.find(T('ModelRef')).set('id', mid.get('id'))
        a.find(T('FromEnd')).find(T('Model')).find(T('ModelProperties')).find(
            "*[@name='EndModelElement']").find(T('ModelRef')).set('id', ma.get('id'))
        a.find(T('ToEnd')).find(T('Model')).find(T('ModelProperties')).find(
            "*[@name='EndModelElement']").find(T('ModelRef')).set('id', mb.get('id'))
        poner_estereotipo(a, estereotipo)
        contenedor_asoc.append(a)
        for m, tag in ((ma, 'FromEndRelationships'), (mb, 'ToEndRelationships')):
            sec = m.find(T(tag))
            if sec is None:
                sec = ET.SubElement(m, T(tag))
            ET.SubElement(sec, T('RelationshipRef'), {'from': ma.get('id'), 'id': a.get('id'), 'to': mb.get('id')})

        c = copy.deepcopy(por_id(TPL_CONECTOR, 'Connector'))
        if puntos is None:
            (xa, ya, wa, ha), (xb, yb, wb, hb) = ra, rb
            if xa + wa <= xb:      # left -> right
                yy = (max(ya, yb) + min(ya + ha, yb + hb)) // 2
                puntos = [(xa + wa, yy), (xb, yy)]
            else:                  # top -> bottom
                xx = (max(xa, xb) + min(xa + wa, xb + wb)) // 2
                puntos = [(xx, ya + ha), (xx, yb)] if ya + ha <= yb else [(xx, ya), (xx, yb + hb)]
        xs, ys = [p[0] for p in puntos], [p[1] for p in puntos]
        c.attrib.update(id=nuevo_id(), model=a.get('id'), name=nombre, **{'from': fa.get('id'), 'to': fb.get('id')},
                        x=str(min(xs)), y=str(min(ys)), width=str(max(1, max(xs) - min(xs))),
                        height=str(max(1, max(ys) - min(ys))), zorder=str(next(self.z) + 100))
        mm = prop(c, 'metaModelElement')
        mm.find(T('ModelRef')).set('id', a.get('id'))
        pts = c.find(T('Points'))
        for p in list(pts):
            pts.remove(p)
        for px, py in puntos:
            ET.SubElement(pts, T('Point'), {'x': str(px), 'y': str(py)})
        poner_prop(c, 'requestResetCaption', 'true')
        for k in ('x', 'y', 'width', 'height'):
            poner_prop(c, k, c.get(k))
        poner_prop(c, 'zOrder', c.get('zorder'))
        cap = c.find(T('Caption'))
        if cap is not None:
            cap.attrib.update(x=str((min(xs) + max(xs)) // 2 - 85), y=str((min(ys) + max(ys)) // 2 - 50), width='170', height='46')
        mv = prop(a, 'masterView')
        mv.find(T('DiagramElementRef')).attrib.update(id=c.get('id'), model=a.get('id'), name=nombre)
        self.d.find(T('Connectors')).append(c)


# DEP-01: what really runs today on the student's computer (verified: iniciar.cmd, vite.config.ts, application.yml,
# mongod --version, Postman/newman run and MongoDB Compass used in the demo).
d1 = Diagrama('DEP-01 Despliegue local (verificado)',
              'Escenario verificado: todo corre en el computador del estudiante, arrancado con iniciar.cmd. '
              'El navegador carga la SPA desde Vite (:5173); Vite reenvía /api a la API Spring Boot (:8080); '
              'la API guarda en MongoDB (:27017, base finanzas). Postman/newman prueba la API y Compass inspecciona la base.')
d1.nodo('pc', 'device', 'Computador del estudiante', 40, 40, 1940, 920,
        tv=[('sistema operativo', 'Windows 11'), ('arranque', 'iniciar.cmd (API, web y navegador)')],
        doc='Equipo físico donde se ejecuta todo el escenario local.')
d1.nodo('nav', 'env', 'Navegador web (Edge / Chrome)', 70, 160, 330, 340, dentro='pc',
        tv=[('url', 'http://localhost:5173')])
d1.nodo('spa', 'artifact', 'SPA cargada\n(index.html + JavaScript)', 95, 340, 280, 120, dentro='nav',
        tv=[('token de acceso', 'en memoria (15 min)'), ('cookie', 'refresh_token HttpOnly (7 días)')])
d1.nodo('vite', 'env', 'Node.js · servidor de desarrollo Vite', 560, 160, 360, 340, dentro='pc',
        tv=[('versión', 'Vite 7.3.8'), ('comando', 'npm run dev'), ('proxy', '/api → http://localhost:8080')])
d1.nodo('front', 'artifact', 'frontend/', 585, 360, 310, 100, dentro='vite',
        tv=[('tecnología', 'React 18.3.1 + TypeScript 5.9.3')])
d1.nodo('jvm', 'env', 'JVM · Java 21', 1080, 160, 380, 340, dentro='pc',
        tv=[('puerto', '8080'), ('perfil', 'local')])
d1.nodo('api', 'artifact', 'finanzas-api', 1100, 340, 340, 130, dentro='jvm',
        tv=[('versión', 'Spring Boot 3.5.16'), ('comando', 'mvnw spring-boot:run (perfil local)'),
            ('salud', 'GET /actuator/health')])
d1.nodo('mongo', 'env', 'MongoDB Server (servicio de Windows)', 1600, 160, 350, 340, dentro='pc',
        tv=[('versión', '9.0.4'), ('uri', 'mongodb://localhost:27017/finanzas')])
d1.nodo('bd', 'artifact', 'Base de datos "finanzas"', 1625, 340, 300, 130, dentro='mongo',
        tv=[('validación', '$jsonSchema por colección'), ('montos', 'Decimal128'), ('índices', 'únicos y TTL')])
d1.nodo('postman', 'env', 'Postman / newman (cliente de pruebas)', 560, 620, 360, 300, dentro='pc',
        tv=[('entorno', 'Finanzas - local')])
d1.nodo('coleccion', 'artifact', 'Colección "Finanzas Personales API"', 580, 760, 320, 120, dentro='postman',
        tv=[('archivo', 'postman/FinanzasPersonales.postman_collection.json'),
            ('resultado', '44 peticiones · 82 aserciones · 0 fallos')])
d1.nodo('compass', 'env', 'MongoDB Compass', 1600, 620, 350, 300, dentro='pc',
        tv=[('uso', 'inspección de documentos e índices')])
d1.nodo('conexion', 'artifact', 'Conexión a la base "finanzas"', 1625, 760, 300, 110, dentro='compass',
        tv=[('uri', 'mongodb://localhost:27017')])
d1.ruta('nav', 'vite', 'HTTP\nlocalhost:5173', 'HTTP')
d1.ruta('vite', 'jvm', 'HTTP · proxy /api\nlocalhost:8080', 'HTTP')
d1.ruta('jvm', 'mongo', 'Protocolo MongoDB\nlocalhost:27017')
d1.ruta('postman', 'jvm', 'HTTP · localhost:8080\n(pruebas de la API)', 'HTTP', [(920, 770), (1270, 770), (1270, 500)])
d1.ruta('compass', 'mongo', 'Protocolo MongoDB\n:27017', None, [(1775, 620), (1775, 500)])

# DEP-02: planned production (render.yaml, vercel.json, backend/Dockerfile). Not deployed with this version.
d2 = Diagrama('DEP-02 Despliegue en producción (planificado, NO VERIFICADO)',
              'Escenario planificado y NO VERIFICADO: el sitio publicado no corresponde a esta versión. '
              'Vercel sirve la SPA y reescribe /api/* hacia Render; Render construye backend/Dockerfile y ejecuta '
              'finanzas-api.jar en una JVM 21; la API usa MongoDB Atlas por TLS y, si se configura, un servidor SMTP.')
d2.nodo('equipo', 'device', 'Equipo del usuario', 40, 300, 340, 320, tv=[('tipo', 'PC o móvil')])
d2.nodo('nav', 'env', 'Navegador web', 65, 410, 290, 190, dentro='equipo')
d2.nodo('spa', 'artifact', 'SPA cargada\n(index.html + JavaScript)', 85, 490, 250, 80, dentro='nav')
d2.nodo('vercel', 'node', 'Vercel · sitio estático', 480, 40, 540, 320,
        tv=[('framework', 'vite'), ('build', 'npm run build'), ('salida', 'dist'),
            ('cabeceras', 'nosniff · X-Frame-Options DENY · strict-origin-when-cross-origin')])
d2.nodo('dist', 'artifact', 'frontend/dist/', 505, 215, 490, 120, dentro='vercel',
        tv=[('reescritura /api', '/api/:path* → https://finanzas-api.onrender.com/api/:path*'),
            ('reescritura SPA', 'resto de rutas → /index.html')])
d2.nodo('render', 'node', 'Render · servicio web "finanzas-api"', 480, 440, 540, 560,
        tv=[('runtime', 'docker (plan free)'), ('rootDir', 'backend'), ('salud', '/actuator/health'),
            ('autoDeploy', 'true')])
d2.nodo('docker', 'env', 'Contenedor Docker (backend/Dockerfile)', 505, 580, 490, 400, dentro='render',
        tv=[('build', 'maven:3.9.11-eclipse-temurin-21'), ('runtime', 'eclipse-temurin:21-jre-alpine, usuario no root'),
            ('puerto', '8080 (EXPOSE)')])
d2.nodo('jvm', 'env', 'JVM · Java 21', 530, 720, 440, 240, dentro='docker',
        tv=[('JAVA_OPTS', '-XX:MaxRAMPercentage=75 -XX:+UseSerialGC')])
d2.nodo('jar', 'artifact', 'finanzas-api.jar (app.jar)', 550, 800, 400, 140, dentro='jvm',
        tv=[('variables', 'MONGODB_URI, JWT_SECRET, FRONTEND_URL, CORS_ORIGINS'),
            ('cookies y correo', 'COOKIE_SECURE=true, COOKIE_SAMESITE=Lax, MAIL_FROM')])
d2.nodo('atlas', 'node', 'MongoDB Atlas · servicio administrado', 1150, 440, 420, 300,
        tv=[('conexión', 'MONGODB_URI (secreto en Render)')])
d2.nodo('mdb', 'env', 'MongoDB', 1175, 550, 370, 170, dentro='atlas')
d2.nodo('bd', 'artifact', 'Base de datos "finanzas"', 1195, 620, 330, 80, dentro='mdb')
d2.nodo('smtp', 'node', 'Servidor SMTP (opcional)', 1150, 840, 420, 160,
        tv=[('variables', 'SPRING_MAIL_HOST, _PORT, _USERNAME, _PASSWORD'),
            ('sin SMTP', 'la API registra que el correo no se envió')])
d2.ruta('equipo', 'vercel', 'HTTPS · 443\n(SPA y /api)', 'HTTPS', [(210, 300), (210, 200), (480, 200)])
d2.ruta('vercel', 'render', 'HTTPS · 443\nreescritura /api/*', 'HTTPS', [(750, 360), (750, 440)])
d2.ruta('jvm', 'mdb', 'Protocolo MongoDB\nsobre TLS', None, [(970, 740), (1080, 740), (1080, 640), (1175, 640)])
d2.ruta('jvm', 'smtp', 'SMTP + STARTTLS', 'SMTP', [(970, 920), (1150, 920)])

tree.write(dst, encoding='UTF-8', xml_declaration=True)
print('ok')
