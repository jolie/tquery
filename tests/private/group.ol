define group
{
  with( groupRequest ){
    .data << bios;
    with( .query ){
      .aggregate << {
        .dstPath = "name.first",
        .srcPath = "name.first"
      };
      .groupBy << {
        .dstPath = "name.first",
        .srcPath = "name.first"
      }
    }
  }
}