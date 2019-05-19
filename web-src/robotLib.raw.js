const displayMap = (objs, map) =>
  map
    .map((col) =>
      col
        .map((id) => {
          if (id) {
            if (objs[id].type_ === 'Wall') return 'wall '
            else return id
          } else {
            return '     '
          }
        })
        .join(' '),
    )

    .join(`\n`)
